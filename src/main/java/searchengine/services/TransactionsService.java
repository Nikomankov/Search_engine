package searchengine.services;

import searchengine.model.Page;
import searchengine.model.Site;

public interface TransactionsService {

    Page savePage(Page page);
    Site saveSite(Site site);
    Page findPage(String path);
    Site findSite(String url);
    boolean updatePage(Page page, Site site);
    boolean updatePageWithLock(Page page);
    boolean updateSite(Site site);
    boolean updateSiteWithLock(Site site);
    String getSiteStatus(Site site);

    void deleteSiteByUrl(String url);
}
