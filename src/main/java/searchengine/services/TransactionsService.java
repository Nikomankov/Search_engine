package searchengine.services;

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
    List<Site> saveAllSites(Iterable<Site> sites);


    //==================== PAGE ====================
    Optional<Page> findPage(String path);
    boolean savePage(Page page, int siteId);


    //==================== LEMMA ====================


    //==================== INDEX ====================
}
