package searchengine.services;

import searchengine.config.SiteConf;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.ForkJoinPool;

public class SiteParse implements Runnable {

    private SortedSet<String> linksSet;
    private SiteConf siteConf;
    private SiteRepository siteRepository;
    private ForkJoinPool pool;
    private TransactionsService transactionsService;
    private Site site;


    public SiteParse(SiteConf site, SiteRepository siteRepository, ForkJoinPool pool, TransactionsService transactionsService){
        linksSet = Collections.synchronizedSortedSet(new TreeSet<>());
        this.siteConf = site;
        this.siteRepository = siteRepository;
        this.pool = pool;
        this.transactionsService = transactionsService;
    }

    //обходить все страницы, начиная с главной, добавлять их адреса, статусы и содержимое в базу данных в таблицу page;
    public void run(){
        Long start = System.currentTimeMillis();
        clearUrl();
        transactionsService.deleteSiteByUrl(siteConf.getUrl());

        this.site = Site.builder()
                .name(siteConf.getName())
                .url(siteConf.getUrl())
                .status(IndexingStatus.INDEXING)
                .statusTime(new Date()).build();

        int id = siteRepository.save(site).getId();
        site.setId(id);

        System.out.println("Thread: " + Thread.currentThread() + site.toString());

        PageTask task = new PageTask(site.getUrl(), site, transactionsService, linksSet);
        pool.submit(task).join();

        long end = System.currentTimeMillis()-start;
        log(end);

        site.setStatusTime(new Date());
        site.setStatus(IndexingStatus.INDEXED);
        siteRepository.save(site);

        //по завершении обхода изменять статус (поле status) на INDEXED;
        //если произошла ошибка и обход завершить не удалось, изменять статус на FAILED и вносить в поле last_error понятную информацию о произошедшей ошибке.
        //this block under the question. Try to intercept end of indexation site
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
