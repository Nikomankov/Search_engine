package searchengine.services;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.config.SiteConf;
import searchengine.config.SitesList;
import searchengine.dto.index.IndexResponse;
import searchengine.exceptions.IndexingAlreadyRunningException;
import searchengine.exceptions.IndexingIsNotRunningException;
import searchengine.model.IndexingStatus;
import searchengine.model.Site;
import searchengine.repositories.SiteRepository;

import java.util.Date;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class IndexServiceImpl implements IndexService{

    @Autowired
    private SiteRepository siteRepository;

    private final SitesList sitesFromConfig;

    @Override
    public IndexResponse start() {

        //check for indexing process is running
        List<Site> sites = siteRepository.findAll();
        for(Site s : sites){
            if(s.getStatus()==IndexingStatus.INDEXING){
                throw new IndexingAlreadyRunningException();
            }
        }
        sites.clear();

        for(SiteConf s : sitesFromConfig.getSites()){
            Optional<Site> siteOptional = siteRepository.findByUrl(s.getUrl());
            //delete site if it is already in DB
            siteOptional.ifPresent(site -> siteRepository.delete(site));
            //create new Site.object and add them into our list
            Site site = Site.builder().name(s.getName()).url(s.getUrl()).status(IndexingStatus.INDEXING).status_time(new Date()).build();
            System.out.println(site);
            siteRepository.save(site);
            sites.add(site);
        }

        startIndexing(sites);

        return new IndexResponse(true);
    }

    @Override
    public IndexResponse stop() {
        List<Site> sites = siteRepository.findAll();
        boolean alreadyIndexing = sites.stream().anyMatch(s->s.getStatus()==IndexingStatus.INDEXING);
        if(alreadyIndexing) {
            stopIndexing();
        } else throw new IndexingIsNotRunningException();
        return new IndexResponse(true);
    }

    @Override
    public IndexResponse indexPage(String url) {

        //check correct url
        //-------------



        return null;
    }


    //here will be indexing sites
    private void startIndexing(List<Site> sites){
        //обходить все страницы, начиная с главной, добавлять их адреса, статусы и содержимое в базу данных в таблицу page;


        //в процессе обхода постоянно обновлять дату и время в поле status_time таблицы site на текущее;


        //по завершении обхода изменять статус (поле status) на INDEXED;


        //если произошла ошибка и обход завершить не удалось, изменять статус на FAILED и вносить в поле last_error понятную информацию о произошедшей ошибке.
    }

    private void stopIndexing(){

    }
}
