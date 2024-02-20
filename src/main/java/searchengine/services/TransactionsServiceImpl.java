package searchengine.services;

import org.hibernate.AssertionFailure;
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
import java.util.ArrayList;
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
    public List<Site> saveAllSites(List<Site> sites) {
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
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void updateSiteTime(Site site) {
        siteRepository.updateDate(site.getId(), new Date(System.currentTimeMillis()));
    }

    /**
     * Final update of the site.
     * Update only two fields so as not to overwrite the "last_error" field.
     * @param site - Parent site
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @Override
    public void updateSiteTimeAndStatus(Site site) {
        siteRepository.updateDateAndStatus(site.getId(), new Date(System.currentTimeMillis()), site.getStatus().toString());
    }


    //==================== PAGE ====================

    @Override
    public Optional<Page> findPage(String path) {
        return pageRepository.findByPath(path);
    }

    /**
     * Saves the page if it has not yet been created and returns true; if it has already been created, it returns false.
     * If exceptions occur, it tries again (up to 5 times).
     * @param page - Current page
     * @return - whether the page was saved
     */
    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10, noRollbackFor = SQLException.class)
    @Override
    public Page savePage(Page page) {
        int retries = 0;
        while (retries < 5){
            try{
                retries++;
                Optional<Page> optionalPage = pageRepository.findByPath(page.getPath());
                if(optionalPage.isEmpty()){
                    return pageRepository.save(page);
                }
            } catch(Exception e){
                StringBuilder builder = new StringBuilder();
                builder.append("\n").append(Thread.currentThread().getName())
                        .append("\n").append(page)
                        .append("\n").append(e.getMessage());
                System.out.println(builder);
            }
        }
        return null;
    }


    //==================== LEMMA ====================
    @Transactional
    @Override
    public Optional<Lemma> findLemma(String lemma, Site site) {
        return lemmaRepository.findByLemmaAndSite(lemma, site);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = {SQLException.class})
    @Override
    public Lemma saveLemma(Lemma lemma) {
        Optional<Lemma> optionalLemma = lemmaRepository.findByLemmaAndSite(lemma.getLemma(), lemma.getSite());
        if(optionalLemma.isPresent()){
            lemma = optionalLemma.get();
            lemma.increaseFrequency();
            return lemmaRepository.save(lemma);
        }
        return lemmaRepository.save(lemma);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW, noRollbackFor = {SQLException.class})
    @Override
    public List<Lemma> saveAllLemmas(List<Lemma> lemmas) {
        List<Lemma> result = new ArrayList<>(lemmas.size());
        for(Lemma l : lemmas){
            Optional<Lemma> optionalLemma = lemmaRepository.findByLemmaAndSite(l.getLemma(), l.getSite());
            if(optionalLemma.isPresent()){
                l = optionalLemma.get();
                l.increaseFrequency();
                 result.add(lemmaRepository.save(l));
            } else {
                result.add(lemmaRepository.save(l));
            }
        }
        return result;
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

    @Transactional(propagation = Propagation.REQUIRES_NEW, timeout = 10, noRollbackFor = {SQLException.class})
    @Override
    public List<IndexM> saveAllIndexes(List<IndexM> indexes) {
        List<IndexM> result = new ArrayList<>(indexes.size());
        IndexM index = null;
        int retries = 0;
        while (retries < 5){
            try{
                retries++;
                for (IndexM i : indexes){
                    Optional<IndexM> optionalIndex = indexRepository.findByPageAndLemma(i.getPage(), i.getLemma());
                    if(optionalIndex.isEmpty()){
                        index = i;
                        result.add(indexRepository.save(i));
                    }
                }
                return result;
            } catch(Exception e){
                StringBuilder builder = new StringBuilder();
                builder.append("\n").append(Thread.currentThread().getName())
                        .append("\n").append(index)
                        .append("\n").append(e.getClass().getName())
                        .append("\n").append(e.getMessage())
                        .append("\nretry = ").append(retries);
                System.out.println(builder);
            }
        }
        return null;

    }



}
