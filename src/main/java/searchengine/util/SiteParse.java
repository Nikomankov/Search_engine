package searchengine.util;

import lombok.SneakyThrows;
import searchengine.config.SiteConf;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.services.TransactionsService;
import java.util.*;
import java.sql.Date;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.ForkJoinPool;

/**
 * Class for running parsing the entire site. Parsing starts from the main page.
 * Each page contains previously unknown links from the parent site
 */
public class SiteParse extends Thread {
    private SortedSet<String> linksSet;
    private final SiteConf siteConf;
    private ForkJoinPool pool;
    private TransactionsService transactionsService;


    public SiteParse(SiteConf site, ForkJoinPool pool, TransactionsService transactionsService){
        this.siteConf = site;
        this.transactionsService = transactionsService;
        this.pool = pool;
        linksSet = Collections.synchronizedSortedSet(new TreeSet<>());
    }

    @SneakyThrows
    public void run(){
        Long start = System.currentTimeMillis();

        clearUrl();
        transactionsService.deleteSiteByUrl(siteConf.getUrl());

        Site site = Site.builder()
                .name(siteConf.getName())
                .url(siteConf.getUrl())
                .status(IndexingStatus.INDEXING)
                .statusTime(new Date(System.currentTimeMillis())).build();

        int id = transactionsService.saveSite(site).getId();
        site.setId(id);

        Site cloneSite = (Site) site.clone();
        System.out.println("URL: " + cloneSite.getUrl());
        PageTask task = new PageTask(cloneSite.getUrl(), cloneSite, linksSet, transactionsService);
        pool.invoke(task);

        long end = System.currentTimeMillis()-start;

        site.setStatusTime(new Date(System.currentTimeMillis()));
        site.setStatus(IndexingStatus.INDEXED);
        transactionsService.updateSiteTimeAndStatus(site);
        log(end);
    }

    /**
     * Clear the source url and change it to "https://www.site.com"
     */
    private void clearUrl(){
        String url = siteConf.getUrl();
        url = url.charAt(url.length()-1) == '/' ? url.substring(0,url.length()-1) : url; //removed "/" at the end
        url = url.replaceFirst("www\\.?", "");
        siteConf.setUrl(url);
    }

    private void log(long time){
        int hour = (int)((int)time/(3.6*Math.pow(10,6)));
        int min = (int)time / 60000;
        int sec = (int)(time % 60000) / 1000;
        StringBuilder log = new StringBuilder("TASK JOIN!");
        log.append("\n\tRuntime = ")
                .append(hour > 0 ? (hour + "h ") : "")
                .append(min > 0 ? (min + "min ") : "")
                .append(sec > 0 ? (sec + "sec ") : "")
                .append(time % 1000).append("msc")
                .append("\nGlobal links amount: ").append(linksSet.size());
        System.out.println(log);
    }
}
