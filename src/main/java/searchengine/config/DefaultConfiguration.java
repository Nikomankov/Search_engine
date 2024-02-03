package searchengine.config;

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
}
