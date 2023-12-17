package searchengine.services;

import searchengine.dto.index.IndexResponse;

public interface IndexService {
    IndexResponse start();

    IndexResponse stop();

    IndexResponse indexPage(String url);
}
