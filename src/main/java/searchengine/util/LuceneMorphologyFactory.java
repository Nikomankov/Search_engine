package searchengine.util;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LuceneMorphologyFactory {

    private Map<LanguageType, LuceneMorphology> luceneMorphologyMap;

    public LuceneMorphologyFactory(){
        luceneMorphologyMap = new HashMap<>(2);
        try {
            luceneMorphologyMap.put(LanguageType.RU, new RussianLuceneMorphology());
            luceneMorphologyMap.put(LanguageType.ENG, new EnglishLuceneMorphology());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LuceneMorphology getLuceneMorphology(LanguageType language){
        return luceneMorphologyMap.get(language);
    }

}
