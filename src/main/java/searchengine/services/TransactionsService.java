package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

import java.util.List;

public interface TransactionsService {

    Site saveSite(Site site);
    Page findPage(String path);
    List<Site> findAllSites();
    List<Site> saveAllSites(Iterable<Site> sites);
    boolean updateOrSavePage(Page page, int siteId);
    boolean updateSite(Site site);
    void deleteSiteByUrl(String url);
}
