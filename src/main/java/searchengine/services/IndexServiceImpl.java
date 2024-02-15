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
import searchengine.repositories.RepositoryFactory;
import searchengine.repositories.SiteRepository;
import searchengine.util.PageTask;
import searchengine.util.SiteParse;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService{

    @Autowired
    private ForkJoinPool pool;
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private RepositoryFactory repositoryFactory;
    private final SitesList sitesFromConfig;
    private final JsoupConnectionConf conf;

    @Override
    public IndexResponse start() {

        List<Site> sites = siteRepository.findAll();
        if(isIndexing(sites)){
            return new IndexResponse(false,"Индексация уже запущена");
        }

        setStartingConfig();

        for(SiteConf s : sitesFromConfig.getSites()){
            System.out.println(s.getName() + ",  " + s.getUrl());
            new SiteParse(s, repositoryFactory, pool).run();
        }
//        List<Future<Boolean>> futures = new ArrayList<>(sitesFromConfig.getSites().size());
//        Boolean result = true;
//        for(SiteConf s : sitesFromConfig.getSites()){
//            System.out.println(s.getName() + ",  " + s.getUrl());
//            futures.add(pool.submit(new SiteParse(s, siteRepository, pool, transactionsService)));
//        }
//        for(Future<Boolean> future : futures){
//            try {
//                result = result & future.get();
//            } catch (InterruptedException e) {
//                throw new RuntimeException(e);
//            } catch (ExecutionException e) {
//                throw new RuntimeException(e);
//            }
//        }

        return new IndexResponse(true);
    }

    @Override
    public IndexResponse stop() {
        List<Site> sites = siteRepository.findAll();

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

        sites = siteRepository.findAll();
        for (Site site : sites){
            if(site.getStatus() == IndexingStatus.INDEXING) {
                site.setStatus(IndexingStatus.FAILED);
                site.setLastError("Принудительное завершение пользователем");
            }
        }
        siteRepository.saveAll(sites);
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
