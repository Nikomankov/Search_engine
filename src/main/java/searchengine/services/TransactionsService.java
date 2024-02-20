package searchengine.services;

import searchengine.model.IndexM;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;
import java.util.Optional;

public interface TransactionsService {


    //==================== SITE ====================
    Optional<Site> findSiteByUrl(String url);
    Site saveSite(Site site);
    void deleteSiteByUrl(String url);
    void updateSiteTime(Site site);
    void updateSiteTimeAndStatus(Site site);
    List<Site> findAllSites();
    List<Site> saveAllSites(List<Site> sites);


    //==================== PAGE ====================
    Optional<Page> findPage(String path);
    Page savePage(Page page);


    //==================== LEMMA ====================
    Optional<Lemma> findLemma(String lemma, Site site);
    Lemma saveLemma(Lemma lemma);
    List<Lemma> saveAllLemmas(List<Lemma> lemmas);

    //==================== INDEX ====================
    Optional<IndexM> findIndexByPageAndLemma(Page page, Lemma lemma);
    IndexM saveIndex(IndexM index);
    List<IndexM> saveAllIndexes(List<IndexM> indexes);
}
