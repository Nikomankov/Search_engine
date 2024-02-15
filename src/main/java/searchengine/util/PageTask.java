package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.RepositoryFactory;
import searchengine.repositories.RepositoryType;
import searchengine.repositories.SiteRepository;

import java.io.IOException;
import java.sql.Date;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.RecursiveTask;

public class PageTask extends RecursiveTask<Boolean> {

    private static String userAgent;
    private static String referrer;
    private final String url;
    private final Site parent;
    private SortedSet<String> globalLinks;
    private RepositoryFactory repositoryFactory;
    private List<PageTask> tasks;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;

    /*
    TODO:
        -insert lemmatization
        -divide the code into methods
     */

    public PageTask(String url, Site parent, SortedSet<String> linksSet, RepositoryFactory repositoryFactory){
        this.url = url;
        this.parent = parent;
        this.globalLinks = linksSet;
        this.repositoryFactory = repositoryFactory;
        tasks = new ArrayList<>();

    }

    @Override
    protected Boolean compute() {
        getRepos();
        String onlyPath = url.replace(parent.getUrl(), "");

        Page page = findPage(onlyPath);
        String errorMessage = null;

        //Check in DB
        if(page != null){
           return false;
        }
        Document pageDoc;
        Connection connection = Jsoup.connect(url)
                .timeout(0)
                .userAgent(userAgent)
                .referrer(referrer)
                .ignoreHttpErrors(true)
                .ignoreContentType(true);

        String statusMessage = "";

        try{
            pageDoc = connection.get();
            int statusCode = connection.response().statusCode();
            statusMessage = connection.response().statusMessage();

            page = Page.builder()
                    .site(parent)
                    .path(onlyPath)
                    .code(statusCode)
                    .build();
            page.setContent(pageDoc.toString());

//            lemmaService.compute(pageDoc, page);

            if(statusCode > 399){
                parent.setLastError(statusCode + " " + statusMessage);
            }
            if(!updateOrSavePage(page, parent.getId()) && updateSite(parent)){
                errorMessage = "Transaction failed";
                return false;
            }

        } catch (RuntimeException | IOException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
            return false;
        }


        findLinks(pageDoc);

        List<CompletableFuture<Boolean>> futures = tasks.stream()
                .map(task -> CompletableFuture.supplyAsync(task::compute))
                .toList();
        CompletableFuture<Boolean> allTasksResult =
                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenApply(
                    v -> futures.stream()
                            .map(CompletableFuture::join)
                            .reduce(true,Boolean::logicalAnd));
        return allTasksResult.join();
//        tasks.forEach(ForkJoinTask::fork);
    }

    private void getRepos(){
        siteRepository = (SiteRepository) repositoryFactory.getRepository(RepositoryType.SITE);
        pageRepository = (PageRepository) repositoryFactory.getRepository(RepositoryType.PAGE);
    }

    public void findLinks(Document pageDoc){
        //find all links elements on page
        Elements elements = pageDoc.select("link");
        elements.addAll(pageDoc.select("a")
                .stream()
                .filter(e -> !e.attr("class")
                .contains("disable")).toList());

        //filter them
        elements.stream()
                .map(e -> e.attr("href"))
                .distinct()
                .filter(this::checkInGlobal)
                .forEach(this::addLink);
    }

    //Checking link to the file link and the presence in the global set
    private boolean checkInGlobal(String link){
        return !globalLinks.contains(link) && !globalLinks.contains(parent.getUrl() + link) && !link.matches("^.+((\\.\\w{1,4})|(/#.*))$");
    }

    //Add link to local set
    private void addLink(String link){
        if(link.matches("^/?[\\w-/]+(/|\\.htm)?$")){   //select lines of the formats "/link/" and link.htm
            if(link.charAt(0) == '/') {
                link = parent.getUrl() + link;
            }
            else {
                link = url + "/" + link;    //add "/" to the start link
            }
        }
        if(link.matches("^" + parent.getUrl() + ".*$")){ //find url
            globalLinks.add(link);
            tasks.add(new PageTask(link, parent, globalLinks, repositoryFactory));
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private Page findPage(String path) {
        Page page = null;
        Optional<Page> optionalPage = pageRepository.findByPath(path);
        if(optionalPage.isPresent()){
            page = optionalPage.get();
        }
        return page;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10, rollbackFor = SQLException.class, noRollbackFor = AssertionError.class)
    public boolean updateOrSavePage(Page page, int siteId) {
        int retries = 0;
        while (retries < 5){
            try{
                retries++;
                Optional<Page> optionalPage = pageRepository.findByPath(page.getPath());
                if(!optionalPage.isPresent()){
                    pageRepository.save(page);
                    return true;
                }
            } catch(Exception e){
                StringBuilder builder = new StringBuilder();
                builder.append(page).append("\n")
                        .append(Thread.currentThread().getName())
                        .append("\n").append(e.getMessage());
                System.out.println(builder);
            }
        }
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    public boolean updateSite(Site site) {
        siteRepository.updateDate(site.getId(), new Date(System.currentTimeMillis()));
        return true;
    }


//    public void logConnectionInfo(Page page, String statusMessage){
//        int code = page.getCode();
//        StringBuilder builder = new StringBuilder()
//                .append("\n\tThread: ").append(Thread.currentThread().getName())
//                .append("\n\tPage: ").append(page.getId())
//                .append("\n\tPath: ").append(page.getPath())
//                .append("\n\tCode: ").append(page.getCode())
//                .append("\n\tStatus message: ").append(statusMessage)
//                .append(errorMessage != null ? ("\n\tError: " + errorMessage) : "");
//
//        if(code > 399){
//            if(code < 500){
//                builder.insert(0,"\nCLIENT ERROR");
//            } else {
//                builder.insert(0,"\nSERVER ERROR");
//            }
//            logger.error(builder);
//        } else {
//            logger.info(builder);
//        }
//    }

    public static void setJsoupConf(String userAgent, String referrer){
        PageTask.userAgent = userAgent;
        PageTask.referrer = referrer;
    }
}
