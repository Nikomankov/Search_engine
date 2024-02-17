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

        createLemmas(pageDoc);

        findLinks(pageDoc);

        tasks.forEach(ForkJoinTask::fork);
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

    private void createLemmas(Document pageDoc){
        Lemmatizator lemmatizator = new Lemmatizator(luceneMorphologyFactory);
        Map<String, Integer> lemmasMap = lemmatizator.compute(pageDoc);
        List<Lemma> lemmas = new ArrayList<>(lemmasMap.size());
        for(String key : lemmasMap.keySet()){
            Lemma lemma = Lemma.builder()
                    .lemma(key)
                    .site(page.getSite())
                    .build();
            lemmas.add(lemma);
        }
        lemmas = transactionsService.saveAllLemmas(lemmas);

        List<IndexM> indexes = new ArrayList<>(lemmas.size());
        for(Lemma l : lemmas){
            IndexM index = IndexM.builder()
                    .page(page)
                    .lemma(l)
                    .rank(lemmasMap.get(l.getLemma()))
                    .build();
            indexes.add(index);
        }
        indexes = transactionsService.saveAllIndexes(indexes);
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
                .filter(this::checkInGlobal)
                .forEach(this::addLink);
    }

    /**
     * We check if the general set of links of the site contain link and if it does not match the template like "css/site.css", "/favicon.ico", etc. (links to files).
     * @param link - link found on page
     * @return - true if it doesn't contain into globalLinks and doesn't math the template
     */
    private boolean checkInGlobal(String link){
        return !globalLinks.contains(link) && !globalLinks.contains(parent.getUrl() + link) && !link.matches("^.+((\\.\\w{1,4})|(/#.*))$");
    }

    /**
     * Select link of "/link/" and "link.htm" format and bring them to form "www.site.com/link".
     * If the resulting link looks like "www.site.com/link" then add to set of global links and create new PageTask.
     * @param link - link found on page
     */
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

    /**
     * Set static connection parameters.
     * @param userAgent - User agent
     * @param referrer - Referrer
     */
    public static void setJsoupConf(String userAgent, String referrer, LuceneMorphologyFactory luceneMorphologyFactory){
        PageTask.userAgent = userAgent;
        PageTask.referrer = referrer;
        PageTask.luceneMorphologyFactory = luceneMorphologyFactory;
    }
}
