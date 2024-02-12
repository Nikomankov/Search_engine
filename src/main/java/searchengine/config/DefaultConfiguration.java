package searchengine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.concurrent.ForkJoinPool;

@Configuration
public class DefaultConfiguration {

    @Bean
    public ForkJoinPool pool(){
        return new ForkJoinPool();
    }
}
