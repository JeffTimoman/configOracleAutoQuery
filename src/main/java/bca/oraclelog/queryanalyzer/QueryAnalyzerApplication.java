package bca.oraclelog.queryanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties
public class QueryAnalyzerApplication {

    public static void main(String[] args) {
        SpringApplication.run(QueryAnalyzerApplication.class, args);
    }

}
