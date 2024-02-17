package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.TransactionsService;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class PageTask extends RecursiveAction {
    private static String userAgent;
    private static String referrer;
    private final String url;
    private final Site parent;
    private SortedSet<String> globalLinks;
    private TransactionsService transactionsService;
    private List<PageTask> tasks;
    private String clearPath;
    private Page page;

    /*
    TODO:
        -insert lemmatization
     */

    public PageTask(String url, Site parent, SortedSet<String> linksSet, TransactionsService transactionsService){
        this.url = url;
        this.parent = parent;
        this.globalLinks = linksSet;
        this.transactionsService = transactionsService;
        tasks = new ArrayList<>();

    }

    @Override
    protected void compute() {
        this.clearPath = url.replace(parent.getUrl(), "");

        Optional<Page> optionalPage = transactionsService.findPage(clearPath);
        if(optionalPage.isPresent()){
           return;
        }

        Document pageDoc = connectToPage();
        if(pageDoc == null){
            return;
        }

        findLinks(pageDoc);

        tasks.forEach(ForkJoinTask::fork);
        tasks.forEach(ForkJoinTask::join);
    }

    private Document connectToPage(){
        Document pageDoc;
        String errorMessage = "";
        Connection connection = Jsoup.connect(url)
                .timeout(0)
                .userAgent(userAgent)
                .referrer(referrer)
                .ignoreHttpErrors(true)
                .ignoreContentType(true);

        try{
            pageDoc = connection.get();
            int statusCode = connection.response().statusCode();
            String statusMessage = connection.response().statusMessage();

            this.page = Page.builder()
                    .site(parent)
                    .path(clearPath)
                    .code(statusCode)
                    .content(pageDoc.toString())
                    .build();

            if(statusCode > 399){
                errorMessage = statusCode + " " + statusMessage;
            }

            if(!transactionsService.savePage(page, parent.getId())){
                errorMessage = "Transaction failed";
                return null;
            }
        } catch (Exception e) {
            errorMessage = e.getMessage();
            return null;
        } finally {
            parent.setLastError(errorMessage);
            transactionsService.updateSiteTimeAndStatus(parent);
        }
        return pageDoc;
    }

    private void findLinks(Document pageDoc){
        //find all links elements on page
        Elements elements = pageDoc.select("link");
        elements.addAll(pageDoc.select("a")
                .stream()
                .filter(e -> !e.attr("class").contains("disable")).
                toList());

        //filter them
        elements.stream()
                .map(e -> e.attr("href"))
                .distinct()
                .filter(this::checkInGlobal)
                .forEach(this::addLink);
    }

    private boolean checkInGlobal(String link){
        return !globalLinks.contains(link) && !globalLinks.contains(parent.getUrl() + link) && !link.matches("^.+((\\.\\w{1,4})|(/#.*))$");
    }

    private void addLink(String link){
        if(link.matches("^/?[\\w-/]+(/|\\.htm)?$")){
            if(link.charAt(0) == '/') {
                link = parent.getUrl() + link;
            }
            else {
                link = url + "/" + link;
            }
        }
        if(link.matches("^" + parent.getUrl() + ".*$")){
            globalLinks.add(link);
            tasks.add(new PageTask(link, parent, globalLinks, transactionsService));
        }
    }

    public static void setJsoupConf(String userAgent, String referrer){
        PageTask.userAgent = userAgent;
        PageTask.referrer = referrer;
    }
}
