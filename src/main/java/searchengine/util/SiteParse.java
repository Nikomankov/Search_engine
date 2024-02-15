package searchengine.util;

import lombok.SneakyThrows;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.config.SiteConf;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.RepositoryFactory;
import searchengine.repositories.RepositoryType;
import searchengine.repositories.SiteRepository;

import java.util.*;
import java.sql.Date;

import java.util.concurrent.ForkJoinPool;

public class SiteParse implements Runnable {

    private SortedSet<String> linksSet;
    private final SiteConf siteConf;
    private RepositoryFactory repositoryFactory;
    private ForkJoinPool pool;
    private SiteRepository siteRepository;
    private PageRepository pageRepository;



    public SiteParse(SiteConf site, RepositoryFactory repositoryFactory, ForkJoinPool pool){
        this.siteConf = site;
        this.repositoryFactory = repositoryFactory;
        this.pool = pool;

        linksSet = Collections.synchronizedSortedSet(new TreeSet<>());
    }

    //обходить все страницы, начиная с главной, добавлять их адреса, статусы и содержимое в базу данных в таблицу page;
    @SneakyThrows
    public void run(){
        Long start = System.currentTimeMillis();

        getRepos();

        clearUrl();
        deleteSite(siteConf.getUrl());

        Site site = Site.builder()
                .name(siteConf.getName())
                .url(siteConf.getUrl())
                .status(IndexingStatus.INDEXING)
                .statusTime(new Date(System.currentTimeMillis())).build();

        int id = siteRepository.save(site).getId();
        site.setId(id);

        System.out.println("Thread: " + Thread.currentThread() + site.toString());

        Site cloneSite = (Site) site.clone();
        PageTask task = new PageTask(cloneSite.getUrl(), cloneSite, linksSet, repositoryFactory);
        pool.submit(task).join();

        long end = System.currentTimeMillis()-start;

        site.setStatusTime(new Date(System.currentTimeMillis()));
        site.setStatus(IndexingStatus.INDEXED);
        siteRepository.save(site);
    }

    private void clearUrl(){
        String url = siteConf.getUrl();
        url = url.charAt(url.length()-1) == '/' ? url.substring(0,url.length()-1) : url; //removed "/" at the end
        siteConf.setUrl(url);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    private void deleteSite(String url){
        Optional<Site> optionalSite = siteRepository.findByUrl(url);
        if(optionalSite.isPresent()){
            Site site = optionalSite.get();
            System.out.println(site);
            pageRepository.deleteBySite(site);
            siteRepository.delete(site);
        }
    }

    private void getRepos(){
        siteRepository = (SiteRepository) repositoryFactory.getRepository(RepositoryType.SITE);
        pageRepository = (PageRepository) repositoryFactory.getRepository(RepositoryType.PAGE);
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
