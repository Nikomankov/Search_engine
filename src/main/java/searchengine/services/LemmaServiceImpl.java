package searchengine.services;

import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import searchengine.model.IndexM;
import searchengine.model.Lemma;
import searchengine.model.Page;
import searchengine.util.LuceneMorphologyFactory;
import searchengine.util.Lemmatizator;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class LemmaServiceImpl implements LemmaService{

    @Autowired
    LuceneMorphologyFactory languagesOfLuceneMorphology;

    /*
    TODO:
        - bath save lemmas to DB. If it does not exist create and save, else increase frequency by 1. Return list of lemmas
        - create indexes and bath save to DB
     */

    public void compute(Document pageDoc, Page page){
        Lemmatizator lemmatizator = new Lemmatizator(languagesOfLuceneMorphology);
        Map<String, Integer> lemmasMap = lemmatizator.compute(pageDoc);
        List<Lemma> lemmas = new ArrayList<>(lemmasMap.size());
        lemmasMap.forEach((key, value) -> {
            Lemma lemma = Lemma.builder()
                    .lemma(key)
                    .site(page.getSite())
                    .build();

            lemmas.add(lemma);

            IndexM index = IndexM.builder()
                    .page(page)
                    .lemma(lemma)
                    .rank(value)
                    .build();
        } );

    }

    public void createLemmas(){

    }
}
