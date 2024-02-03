package searchengine.services;

import lombok.SneakyThrows;
import searchengine.config.SiteConf;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;

import java.util.*;
import java.sql.Date;

import java.util.concurrent.ForkJoinPool;

public class SiteParse implements Runnable {

    private SortedSet<String> linksSet;
    private final SiteConf siteConf;
    private final ForkJoinPool pool;
    private TransactionsService transactionsService;


    public SiteParse(SiteConf site, ForkJoinPool pool, TransactionsService transactionsService){
        linksSet = Collections.synchronizedSortedSet(new TreeSet<>());
        this.siteConf = site;
        this.pool = pool;
        this.transactionsService = transactionsService;
    }

    //обходить все страницы, начиная с главной, добавлять их адреса, статусы и содержимое в базу данных в таблицу page;
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

        System.out.println("Thread: " + Thread.currentThread() + site.toString());

        Site cloneSite = (Site) site.clone();
        PageTask task = new PageTask(cloneSite.getUrl(), cloneSite, transactionsService, linksSet);
        pool.submit(task).join();

//        long end = System.currentTimeMillis()-start;
//        log(end);

//        site.setStatusTime(new Date(System.currentTimeMillis()));
//        site.setStatus(IndexingStatus.INDEXED);
//        transactionsService.saveSite(site);
    }

    private void clearUrl(){
        String url = siteConf.getUrl();
        url = url.charAt(url.length()-1) == '/' ? url.substring(0,url.length()-1) : url; //removed "/" at the end
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
