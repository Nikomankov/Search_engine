package searchengine.services;

import org.jsoup.nodes.Document;
import searchengine.model.Page;

public interface LemmaService {

    void compute(Document pageDoc, Page page);
}
