package bca.oraclelog.queryanalyzer.config;

import bca.oraclelog.queryanalyzer.interceptor.OracleQueryInterceptor;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "query.analyzer.enabled", havingValue = "true")
public class QueryAnalyzerConfig {
    
    @Autowired
    private OracleQueryInterceptor queryInterceptor;
    
    @Bean
    @Primary
    public DataSource proxyDataSource(DataSource actualDataSource) {
        ChainListener listener = new ChainListener();
        listener.addListener(new DataSourceQueryCountListener());
        listener.addListener(queryInterceptor);
        
        return ProxyDataSourceBuilder
                .create(actualDataSource)
                .name("QueryAnalyzerDS")
                .listener(listener)
                .build();
    }
}