package searchengine.services;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import searchengine.model.Page;
import searchengine.model.Site;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveTask;
import java.util.concurrent.atomic.AtomicInteger;

public class PageTask extends RecursiveTask<Boolean> {

    private static final Logger logger = LogManager.getLogger("parserTaskLogger");

    private static String userAgent;
    private static String referrer;

    private SortedSet<String> globalLinks;
    private String url;
    private Site parent;
    private TransactionsService transactionsService;

    private TreeSet<String> localLinks;
    private String errorMessage = null;
    private List<PageTask> tasks;

    public PageTask(String url, Site parent, TransactionsService transactionsService, SortedSet<String> linksSet){
        this.parent= parent;
        this.transactionsService = transactionsService;
        this.globalLinks = linksSet;
        this.url = url;
        localLinks = new TreeSet<>();
        tasks = new ArrayList<>();
    }

    @Override
    protected Boolean compute() {

        Page page = transactionsService.findPage(url);

        //Check in DB
        if(page != null){
           return false;
        }
        Document pageDoc;
        boolean connected = false;
        Connection connection = Jsoup.connect(url)
                .timeout(20000)
                .userAgent(userAgent)
                .referrer(referrer)
                .ignoreHttpErrors(true)
                .ignoreContentType(true);
//                    .followRedirects(false);


        String statusMessage = "";

        try{
            pageDoc = connection.get();
            int statusCode = connection.response().statusCode();
            statusMessage = connection.response().statusMessage();

            page = Page.builder()
                    .site(parent)
                    .path(url)
                    .code(statusCode)
                    .build();
            page.setContent(pageDoc.toString());

            if(statusCode > 399){
                parent.setLastError(statusCode + " " + statusMessage);
            }
            parent.setStatusTime(new Date());
            if(!transactionsService.updatePage(page, parent)){
                errorMessage = "Transaction failed";
                return false;
            }

//            transactionsService.updateSite(parent);
        } catch (RuntimeException | IOException e) {
            errorMessage = e.getMessage();
            e.printStackTrace();
            return false;
        } finally {

        }


        findLinks(pageDoc);

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
        return true;
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


    public void logConnectionInfo(Page page, String statusMessage){
        int code = page.getCode();
        StringBuilder builder = new StringBuilder()
                .append("\n\tThread: ").append(Thread.currentThread().getName())
                .append("\n\tPage: ").append(page.getId())
                .append("\n\tPath: ").append(page.getPath())
                .append("\n\tCode: ").append(page.getCode())
                .append("\n\tStatus message: ").append(statusMessage)
                .append(errorMessage != null ? ("\n\tError: " + errorMessage) : "");

        if(code > 399){
            if(code < 500){
                builder.insert(0,"\nCLIENT ERROR");
            } else {
                builder.insert(0,"\nSERVER ERROR");
            }
            logger.error(builder);
        } else {
            logger.info(builder);
        }
    }

    public static void setJsoupConf(String userAgent, String referrer){
        PageTask.userAgent = userAgent;
        PageTask.referrer = referrer;
    }
}
