package bca.oraclelog.queryanalyzer.config;

import bca.oraclelog.queryanalyzer.interceptor.OracleQueryInterceptor;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "query.analyzer.enabled", havingValue = "true")
public class QueryAnalyzerConfig {
    
    @Bean
    @Qualifier("actualDataSource")
    public DataSource actualDataSource(DataSource originalDataSource) {
        return originalDataSource;
    }
    
    @Bean
    @Primary
    public DataSource proxyDataSource(@Qualifier("actualDataSource") DataSource actualDataSource, 
                                    ApplicationContext applicationContext) {
        ChainListener listener = new ChainListener();
        listener.addListener(new DataSourceQueryCountListener());
        
        // Get the interceptor bean from application context to avoid circular dependency
        OracleQueryInterceptor queryInterceptor = applicationContext.getBean(OracleQueryInterceptor.class);
        listener.addListener(queryInterceptor);
        
        return ProxyDataSourceBuilder
                .create(actualDataSource)
                .name("QueryAnalyzerDS")
                .listener(listener)
                .build();
    }
}