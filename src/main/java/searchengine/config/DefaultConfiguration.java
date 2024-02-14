package searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searchengine.util.LanguagesOfLuceneMorphology;
import searchengine.util.Lemmatizator;

import java.io.IOException;
import java.util.concurrent.ForkJoinPool;

@Configuration
public class DefaultConfiguration {

    @Bean
    public ForkJoinPool pool(){
        return new ForkJoinPool();
    }

    @Bean
    public LanguagesOfLuceneMorphology languagesOfLuceneMorphology() {
        return new LanguagesOfLuceneMorphology();
    }
}
