package searchengine.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.EnableTransactionManagement;

import java.util.Collections;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.ForkJoinPool;

@Configuration@EnableTransactionManagement
public class DefaultConfiguration {

    @Bean
    public ForkJoinPool pool(){
        return new ForkJoinPool();
    }

    @Bean
    public SortedSet<String> sortedPagesSet(){
        return Collections.synchronizedSortedSet(new TreeSet<>());
    }

    @Bean
    public Logger parserTaskLogger(){
        return LogManager.getLogger("parserTaskLogger");
    }

    @Bean
    public Logger mainLogger(){
        return LogManager.getLogger("mainLogger");
    }

}