package searchengine.util;

import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;
import searchengine.model.IndexM;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.services.TransactionsService;

import java.util.*;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.RecursiveAction;

/**
 * Class for parsing pages with text indexing by lemmas, searching for links on the parent site,
 * checking them and launching further parsing on them
 */
public class PageTask extends RecursiveAction {
    private static String userAgent;
    private static String referrer;
    private static LuceneMorphologyFactory luceneMorphologyFactory;
    private SortedSet<String>  globalLinks;
    private TransactionsService transactionsService;
    private final String url;
    private final Site parent;
    private String clearPath;
    private Page page;
    Set<String> localLinks;
    private List<PageTask> tasks;
    /*
    TODO:
        -Add batch save lemmas and indexes to DB
     */

    public PageTask(String url, Site parent, SortedSet<String> linksSet, TransactionsService transactionsService){
        this.url = url;
        this.parent = parent;
        this.globalLinks = linksSet;
        this.transactionsService = transactionsService;
        localLinks = new TreeSet<>();
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

        createLemmas(pageDoc);

        findLinks(pageDoc);
        globalLinks.addAll(localLinks);
        System.out.println("\n" + url);
        System.out.println("Local links amount: " + localLinks.size());
        System.out.println("Global links amount: " + globalLinks.size());
        localLinks.forEach(l -> {
            PageTask task = new PageTask(l, parent, globalLinks, transactionsService);
            task.fork();
            tasks.add(task);
        });

        tasks.forEach(ForkJoinTask::join);
    }

    /**
     * Connect to page.
     * Create page.
     * Write errors text to last_error of the site and update time_status.
     * Saves the page and updates the site in the repository.
     * @return - Document of page
     */
    private Document connectToPage(){
        Document pageDoc;
        String errorMessage = "";
        Connection connection = Jsoup.connect(url)
                .timeout(10000)
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
            page = transactionsService.savePage(page);
            if(page == null){
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

    private void createLemmas(Document pageDoc){
        Lemmatizator lemmatizator = new Lemmatizator(luceneMorphologyFactory);
        Map<String, Integer> lemmasMap = lemmatizator.compute(pageDoc);

        try{
            for(String key : lemmasMap.keySet()){
                Lemma lemma = Lemma.builder()
                        .lemma(key)
                        .frequency(1)
                        .site(page.getSite())
                        .build();
                lemma = transactionsService.saveLemma(lemma);

                IndexM index = IndexM.builder()
                        .page(page)
                        .lemma(lemma)
                        .rank(lemmasMap.get(key))
                        .build();
                transactionsService.saveIndex(index);
            }

        } catch (Exception e){
            System.out.println(e);
        }
    }

    /**
     * Find all tags "link" and "a" with active links.
     * Extract the contents of the "href" attribute and filter.
     * @param pageDoc - Document of page
     */
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
                .filter(this::notFileLink)
                .filter(this::notFoundInGlobal)
                .map(this::getNormalForm)
                .filter(this::belongToTheParent)
                .forEach(l -> localLinks.add(l));
    }

    /**
     * Check for it does not match the template like "css/site.css", "/favicon.ico", etc. (links to files).
     * @param link - link found on page
     * @return true if not match
     */
    private boolean notFileLink(String link){
        return !link.matches("^.+((\\.\\w{1,4})|(/#.*))$");
    }

    /**
     * Select link of "/link/" and "link.htm" format and bring them to form "https://site.com/link".
     * @param link - link found on page
     * @return updated link
     */
    private String getNormalForm(String link){
        if(link.matches("^/?[\\w-/]+(/|\\.htm)?$")){
            link = link.charAt(link.length()-1) == '/' ? link : (link + "/");
            link = parent.getUrl() + (link.charAt(0) == '/' ? link :  ("/" + link));
        }
        return link;
    }

    /**
     *
     * does the link contain a parent url, like "https://site.com/link"
     * @param link - link found on page
     * @return true if belong to the parent site
     */
    private boolean belongToTheParent(String link){
        return link.matches("^" + parent.getUrl() + ".*$");
    }

    /**
     * Check if the general set of links of the site contain link
     * @param link  - link found on page
     * @return true if not contain
     */
    private boolean notFoundInGlobal(String link){
        return !globalLinks.contains(link) && !globalLinks.contains(parent.getUrl() + link);
    }

    /**
     * Set static connection parameters.
     * @param userAgent - User agent
     * @param referrer - Referrer
     */
    public static void setConf(String userAgent, String referrer, LuceneMorphologyFactory luceneMorphologyFactory){
        PageTask.userAgent = userAgent;
        PageTask.referrer = referrer;
        PageTask.luceneMorphologyFactory = luceneMorphologyFactory;
    }
}
