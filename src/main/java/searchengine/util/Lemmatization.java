package searchengine.util;

import org.apache.lucene.morphology.LuceneMorphology;
import org.apache.lucene.morphology.WrongCharaterException;
import org.apache.lucene.morphology.english.EnglishLuceneMorphology;
import org.apache.lucene.morphology.russian.RussianLuceneMorphology;
import org.jsoup.nodes.Document;

import java.io.IOException;
import java.util.*;

public class Lemmatization {

    /*
    TODO:
        - think about how to make morph.info files loaded once when compiling the program
        - own tags remover which added space between tags text
     */

    private enum Language {RU, ENG}
    private static final String[] ruAuxPartOfSpeech = new String[]{"ПРЕДЛ", "СОЮЗ", "ЧАСТ", "МЕЖД"};
    private static final String[] engAuxPartOfSpeech = new String[]{"CONJ", "PREP", "ARTICLE", "INT", "PART"};

    public static Map<String, Integer> compute(Document pageDoc) throws IOException {
        Map<String, Integer> result;
        String text = removeTags(pageDoc);
        String[] words = split(text);
        
        List<String> engWords = new ArrayList<>();
        List<String> ruWords = new ArrayList<>();
        for(String w : words){
            if(w.matches("[a-zA-Z]+")){
                engWords.add(w);
            } else {
                ruWords.add(w);
            }
        }

        result = removeAuxPartsOfSpeech(engWords, Language.ENG);
        result.putAll(removeAuxPartsOfSpeech(ruWords, Language.RU));
        return result;
    }

    public static Map<String, Integer> test(String text) throws IOException {
        Map<String, Integer> result;
        String[] words = split(text);
        List<String> engWords = new ArrayList<>();
        List<String> ruWords = new ArrayList<>();
        for(String w : words){
            if(w.matches("[a-zA-Z]+")){
                engWords.add(w);
            } else if(w.matches("[а-яА-ЯёЁ]+")){
                ruWords.add(w);
            }
        }
        result = removeAuxPartsOfSpeech(engWords, Language.ENG);
        result.putAll(removeAuxPartsOfSpeech(ruWords, Language.RU));
        return result;
    }
    private static String removeTags(Document pageDoc){
        return (pageDoc.title() + " " + pageDoc.body().text());
    }

    private static String[] split(String text){
        text = text.replaceAll("[^a-zA-Zа-яА-ЯёЁ\\s]"," ")
                    .replaceAll("[\\s]+"," ")
                    .toLowerCase();
        System.out.println(text);
        return text.split(" ");
    }

    private static Map<String, Integer> removeAuxPartsOfSpeech(List<String> words, Language language) throws IOException {
        Map<String, Integer> result = new HashMap<>(words.size());
        LuceneMorphology luceneMorphology = null;
        String[] auxPartsOfSpeech = null;

        switch (language){
            case RU -> {
                luceneMorphology = new RussianLuceneMorphology();
                auxPartsOfSpeech = ruAuxPartOfSpeech;
            }
            case ENG -> {
                luceneMorphology = new EnglishLuceneMorphology();
                auxPartsOfSpeech = engAuxPartOfSpeech;
            }
        }


        for(String w : words){
            try{
                System.out.println(w);

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

    private static boolean isAuxPartOfSpeech(List<String> morphInfo, String[] auxPartOfSpeech){
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
