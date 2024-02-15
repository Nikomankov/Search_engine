package searchengine.repositories;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;

@Component
public class RepositoryFactory {

    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;

    public RepositoryFactory(){

    }
    @NonNull
    public JpaRepository getRepository(RepositoryType type){
        JpaRepository result = null;
        switch (type){
            case SITE -> result = siteRepository;
            case PAGE -> result = pageRepository;
        }
        return result;
    }

}
