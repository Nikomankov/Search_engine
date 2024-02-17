package searchengine.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import searchengine.model.IndexM;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;
import searchengine.repositories.IndexMRepository;
import searchengine.repositories.LemmaRepository;
import searchengine.repositories.PageRepository;
import searchengine.repositories.SiteRepository;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.sql.Date;

@Service
public class TransactionsServiceImpl implements TransactionsService{
    @Autowired
    private SiteRepository siteRepository;
    @Autowired
    private PageRepository pageRepository;
    @Autowired
    private LemmaRepository lemmaRepository;
    @Autowired
    private IndexMRepository indexRepository;


    //==================== SITE ====================
    @Override
    public Optional<Site> findSiteByUrl(String url){
        return siteRepository.findByUrl(url);
    }

    @Override
    public Site saveSite(Site site) {
        return siteRepository.save(site);
    }

    @Override
    public List<Site> findAllSites() {
        return siteRepository.findAll();
    }

    @Override
    public List<Site> saveAllSites(Iterable<Site> sites) {
        return siteRepository.saveAll(sites);
    }

    /**
     * Deleting a site and all pages related to it.
     * @param url - url of the site
     */
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

    /**
     * Updating "status_time" of the site to the current one.
     * @param site - Parent site
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    @Override
    public void updateSiteTime(Site site) {
        siteRepository.updateDate(site.getId(), new Date(System.currentTimeMillis()));
    }

    /**
     * Final update of the site.
     * Update only two fields so as not to overwrite the "last_error" field.
     * @param site - Parent site
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, isolation = Isolation.REPEATABLE_READ)
    @Override
    public void updateSiteTimeAndStatus(Site site) {
        siteRepository.updateDateAndStatus(site.getId(), new Date(System.currentTimeMillis()), site.getStatus().toString());
    }


    //==================== PAGE ====================

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public Optional<Page> findPage(String path) {
        return pageRepository.findByPath(path);
    }

    /**
     * Saves the page if it has not yet been created and returns true; if it has already been created, it returns false.
     * If exceptions occur, it tries again (up to 5 times).
     * @param page - Current page
     * @param siteId - parent site id
     * @return - whether the page was saved
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10, rollbackFor = SQLException.class, noRollbackFor = AssertionError.class)
    @Override
    public boolean savePage(Page page, int siteId) {
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


    //==================== LEMMA ====================
    @Override
    public Optional<Lemma> findLemma(String lemma) {
        return lemmaRepository.findByLemma(lemma);
    }

    @Override
    public Lemma saveLemma(Lemma lemma) {
        return lemmaRepository.save(lemma);
    }

    @Override
    public List<Lemma> saveAllLemmas(Iterable<Lemma> lemmas) {
        for(Lemma l : lemmas){
            Optional<Lemma> optionalLemma = lemmaRepository.findByLemma(l.getLemma());
            if(optionalLemma.isPresent()){
                l = optionalLemma.get();
                l.increaseFrequency();
                lemmaRepository.save(l);
            } else {
                l = lemmaRepository.save(l);
            }
        }
        return (List<Lemma>) lemmas;
    }


    //==================== INDEX ====================
    @Override
    public Optional<IndexM> findIndexByPageAndLemma(Page page, Lemma lemma) {
        return indexRepository.findByPageAndLemma(page, lemma);
    }

    @Override
    public IndexM saveIndex(IndexM index) {
        return indexRepository.save(index);
    }

    @Override
    public List<IndexM> saveAllIndexes(Iterable<IndexM> indexes) {
        for (IndexM i : indexes){
            Optional<IndexM> optionalIndex = indexRepository.findByPageAndLemma(i.getPage(), i.getLemma());
            if(optionalIndex.isEmpty()){
                i = indexRepository.save(i);
            }
        }
        return (List<IndexM>) indexes;
    }



}
