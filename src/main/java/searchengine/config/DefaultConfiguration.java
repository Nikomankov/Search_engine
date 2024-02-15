package searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import searchengine.util.LuceneMorphologyFactory;

import java.util.concurrent.ForkJoinPool;

@Configuration
public class DefaultConfiguration {

    @Bean
    public ForkJoinPool pool(){
        return new ForkJoinPool();
    }

    @Bean
    public LuceneMorphologyFactory languagesOfLuceneMorphology() {
        return new LuceneMorphologyFactory();
    }
}
