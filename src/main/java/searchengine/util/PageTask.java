package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.TransactionsService;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

public class PageTask extends RecursiveAction {

    private static String userAgent;
    private static String referrer;

    private SortedSet<String> globalLinks;
    private final String url;
    private final Site parent;
    private TransactionsService transactionsService;
    private List<PageTask> tasks;

    public PageTask(String url, Site parent, TransactionsService transactionsService, SortedSet<String> linksSet){
        this.parent= parent;
        this.transactionsService = transactionsService;
        this.globalLinks = linksSet;
        this.url = url;
        tasks = new ArrayList<>();
    }

    @Override
    protected void compute() {
        String onlyPath = url.replace(parent.getUrl(), "");

        Page page = transactionsService.findPage(onlyPath);
        String errorMessage = null;

        //Check in DB
        if(page != null){
           return;
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

            Lemmatization.compute(pageDoc);

            if(statusCode > 399){
                parent.setLastError(statusCode + " " + statusMessage);
            }
            if(!transactionsService.updatePage(page, parent.getId()) &&
                    transactionsService.updateSite(parent)){
                errorMessage = "Transaction failed";
                return;
            }

        } catch (RuntimeException | IOException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
            return;
        }


        findLinks(pageDoc);
//        logConnectionInfo(page,statusMessage);

//        List<CompletableFuture<Boolean>> futures = tasks.stream()
//                .map(task -> CompletableFuture.supplyAsync(task::compute))
//                .toList();
//
//
//
//        CompletableFuture<Boolean> allTasksResult =
//                CompletableFuture.allOf(futures.toArray(new CompletableFuture[futures.size()])).thenApply(
//                    v -> futures.stream()
//                            .map(CompletableFuture::join)
//                            .reduce(true,Boolean::logicalAnd));
//        System.out.println("Task time is " + new Date() + "\nSite status: " + transactionsService.getSiteStatus(parent));
//        return allTasksResult.join();
        tasks.forEach(ForkJoinTask::fork);
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
            tasks.add(new PageTask(link, parent, transactionsService, globalLinks));
        }
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
