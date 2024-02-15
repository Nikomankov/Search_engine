package searchengine.util;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.jsoup.nodes.Document;

import java.util.*;

public class Lemmatizator {

    /*
    TODO:
        - think about how to make morph.info files loaded once when compiling the program
        - own tags remover which added space between tags text
        - add log undefined words to file undefinedWords.log like
            "Time: 00:00:00
             Thread: thread name
             Site: site name
             Page: https://www.site.com/some/path
             Undefined words: first word, second word, third word ...
             "
        - add log with lemmatization progress to file lemmatizationProgress.log like
            "Time: 00:00:00
             Thread: thread name
             Site: site name
             Page: https://www.site.com/some/path
             Lemmas sum: 105
             Aux part of speech sum: 15
             Undefined words sum: 3
             "
     */

    private static final String[] ruAuxPartOfSpeech = new String[]{"ПРЕДЛ", "СОЮЗ", "ЧАСТ", "МЕЖД"};
    private static final String[] engAuxPartOfSpeech = new String[]{"CONJ", "PREP", "ARTICLE", "INT", "PART"};

    private LuceneMorphology engLuceneMorphology;
    private LuceneMorphology ruLuceneMorphology;
    private List<String> engWords;
    private List<String> ruWords;
    private List<String> undefinedWords;




    public Lemmatizator(LuceneMorphologyFactory languages){
        engLuceneMorphology = languages.getLuceneMorphology(LanguageType.ENG);
        ruLuceneMorphology = languages.getLuceneMorphology(LanguageType.RU);
        engWords = new ArrayList<>();
        ruWords = new ArrayList<>();
        undefinedWords = new ArrayList<>();
    }

    public Map<String, Integer> compute(Document pageDoc){
        Map<String, Integer> result;
        String text = removeTags(pageDoc);
        String[] words = split(text);

        for(String w : words){
            if(w.matches("[a-zA-Z]+")){
                engWords.add(w);
            } else if(w.matches("[а-яА-ЯёЁ]+")){
                ruWords.add(w);
            } else {
                undefinedWords.add(w);
            }
        }

        result = removeAuxPartsOfSpeech(engWords, LanguageType.ENG);
        result.putAll(removeAuxPartsOfSpeech(ruWords, LanguageType.RU));
        return result;
    }

    public Map<String, Integer> test(String text) {
        Map<String, Integer> result;
        String[] words = split(text);

        for(String w : words){
            if(w.matches("[a-zA-Z]+")){
                engWords.add(w);
            } else if(w.matches("[а-яА-ЯёЁ]+")){
                ruWords.add(w);
            } else {
                undefinedWords.add(w);
            }
        }
        result = removeAuxPartsOfSpeech(engWords, LanguageType.ENG);
        result.putAll(removeAuxPartsOfSpeech(ruWords, LanguageType.RU));
        return result;
    }

    private String removeTags(Document pageDoc){
        return (pageDoc.title() + " " + pageDoc.body().text());
    }

    private String[] split(String text){
        text = text.replaceAll("[^a-zA-Zа-яА-ЯёЁ\\s]"," ")
                    .replaceAll("[\\s]+"," ")
                    .toLowerCase();
        System.out.println(text);
        return text.split(" ");
    }

    private Map<String, Integer> removeAuxPartsOfSpeech(List<String> words, LanguageType language){
        Map<String, Integer> result = new HashMap<>(words.size());
        LuceneMorphology luceneMorphology = null;
        String[] auxPartsOfSpeech = null;

        switch (language){
            case RU -> {
                luceneMorphology = ruLuceneMorphology;
                auxPartsOfSpeech = ruAuxPartOfSpeech;
            }
            case ENG -> {
                luceneMorphology = engLuceneMorphology;
                auxPartsOfSpeech = engAuxPartOfSpeech;
            }
        }


        for(String w : words){
            try{

                List<String> morphInfo = luceneMorphology.getMorphInfo(w);
                if(isAuxPartOfSpeech(morphInfo, auxPartsOfSpeech)){
                    continue;
                }

                List<String> normalForms = luceneMorphology.getNormalForms(w);
                if(normalForms.isEmpty()){
                    continue;
                }

                String normalWord = normalForms.get(0);
                result.computeIfPresent(normalWord, (key,value) -> value+1);
                result.putIfAbsent(normalWord,1);
            } catch (WrongCharaterException e){
                System.out.println(e.getMessage());
            }
        }
        return result;
    }

    private boolean isAuxPartOfSpeech(List<String> morphInfo, String[] auxPartOfSpeech){
        for(String w : morphInfo){
            for(String p : auxPartOfSpeech){
                if(w.contains(p)){
                    return true;
                }
            }
        }
        return false;
    }

    
    
}
