package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.JsoupConnectionConf;
import searchengine.config.SiteConf;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexResponse;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.util.PageTask;
import searchengine.util.SiteParse;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService{

    private final List<Thread> threads = new ArrayList<>();
    private static Thread waitingThread;
    @Autowired
    private ForkJoinPool pool;
    @Autowired
    private TransactionsService transactionsService;
    private final SitesList sitesFromConfig;
    private final JsoupConnectionConf conf;

    @Override
    public IndexResponse start() {

        List<Site> sites = transactionsService.findAllSites();
        if(isIndexing(sites)){
            return new IndexResponse(false,"Индексация уже запущена");
        }

        setStartingConfig();

        for(SiteConf s : sitesFromConfig.getSites()){
            System.out.println(s.getName() + ",  " + s.getUrl());
            SiteParse siteParse = new SiteParse(s, pool, transactionsService);
            threads.add(siteParse);
            siteParse.start();
        }

        waitingThread = new Thread(() -> {
            threads.forEach(t -> {
                try {
                    t.join();
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            });
        });
        waitingThread.start();

        return new IndexResponse(true);
    }

    @Override
    public IndexResponse stop() {
        List<Site> sites = transactionsService.findAllSites();

        if(!isIndexing(sites)) {
            return new IndexResponse(false);
        }

        pool.shutdownNow();
        while (pool.isTerminating()){
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            System.out.println("Wait");
        }
        System.out.println("Shutdown running");

        sites = transactionsService.findAllSites();
        for (Site site : sites){
            if(site.getStatus() == IndexingStatus.INDEXING) {
                site.setStatus(IndexingStatus.FAILED);
                site.setLastError("Принудительное завершение пользователем");
            }
        }
        transactionsService.saveAllSites(sites);
        return new IndexResponse(true);
    }

    @Override
    public IndexResponse indexPage(String url) {
        return null;
    }

    private boolean isIndexing(List<Site> sites){
        return sites.stream().anyMatch(s -> s.getStatus() == IndexingStatus.INDEXING);
    }

    private void setStartingConfig(){
        PageTask.setJsoupConf(conf.getUserAgent(), conf.getReferrer());
        if(pool.isShutdown()){
            pool = new ForkJoinPool();
        }
    }
}
