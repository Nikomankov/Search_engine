package searchengine.services;

import jakarta.persistence.EntityManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.CannotAcquireLockException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.sql.Date;
import java.util.concurrent.ForkJoinPool;

@Service
public class TransactionsServiceImpl implements TransactionsService{


    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private ForkJoinPool pool;
    @Autowired
    private EntityManager entityManager;


    @Override
    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void deleteSiteByUrl(String url) {
        Optional<Site> optionalSite = siteRepository.findByUrl(url);
        if(optionalSite.isPresent()){
            Site site = optionalSite.get();
            System.out.println(site);
            pageRepository.deleteBySite(site);
            siteRepository.delete(site);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10, rollbackFor = SQLException.class, noRollbackFor = AssertionError.class)
    @Override
    public boolean updateOrSavePage(Page page, int siteId) {
        int retries = 0;
        while (retries < 5){
            try{
                retries++;
                Optional<Page> optionalPage = pageRepository.findByPath(page.getPath());
                if(!optionalPage.isPresent()){
                    pageRepository.save(page);
                    return true;
                }
            } catch(Exception e){
                StringBuilder builder = new StringBuilder();
                builder.append(page).append("\n")
                        .append(Thread.currentThread().getName())
                        .append("\n").append(e.getMessage());
                System.out.println(builder);
            }
        }
        return false;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Page findPage(String path) {
        Page page = null;
        Optional<Page> optionalPage = pageRepository.findByPath(path);
        if(optionalPage.isPresent()){
            page = optionalPage.get();
        }
        return page;
    }


    @Override
    public List<Site> findAllSites() {
        return siteRepository.findAll();
    }

    @Override
    public List<Site> saveAllSites(Iterable<Site> sites) {
        return siteRepository.saveAll(sites);
    }


    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    @Override
    public boolean updateSite(Site site) {
        siteRepository.updateDate(site.getId(), new Date(System.currentTimeMillis()));
        return true;
    }
}
