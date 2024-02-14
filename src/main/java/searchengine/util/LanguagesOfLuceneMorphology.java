package searchengine.util;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class LanguagesOfLuceneMorphology {

    private Map<Language, LuceneMorphology> luceneMorphologyMap;

    public LanguagesOfLuceneMorphology(){
        luceneMorphologyMap = new HashMap<>(2);
        try {
            luceneMorphologyMap.put(Language.RU, new RussianLuceneMorphology());
            luceneMorphologyMap.put(Language.ENG, new EnglishLuceneMorphology());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public LuceneMorphology getLuceneMorphology(Language language){
        return luceneMorphologyMap.get(language);
    }

}
