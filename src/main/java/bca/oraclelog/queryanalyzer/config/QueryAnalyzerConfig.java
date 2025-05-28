package bca.oraclelog.queryanalyzer.config;

import bca.oraclelog.queryanalyzer.interceptor.OracleQueryDebugInterceptor;
import net.ttddyy.dsproxy.listener.ChainListener;
import net.ttddyy.dsproxy.listener.DataSourceQueryCountListener;
import net.ttddyy.dsproxy.support.ProxyDataSourceBuilder;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
@ConditionalOnProperty(name = "oracle.query.debug.enabled", havingValue = "true")
public class QueryAnalyzerConfig {
    
    @Bean
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties dataSourceProperties() {
        return new DataSourceProperties();
    }
    
    @Bean("originalDataSource")
    public DataSource originalDataSource(DataSourceProperties dataSourceProperties) {
        return dataSourceProperties.initializeDataSourceBuilder().build();
    }
    
    @Bean
    @Primary
    public DataSource proxyDataSource(DataSourceProperties dataSourceProperties) {
        // Create the original DataSource directly
        DataSource originalDataSource = dataSourceProperties.initializeDataSourceBuilder().build();
        
        ChainListener listener = new ChainListener();
        listener.addListener(new DataSourceQueryCountListener());
        
        // Create interceptor with the original DataSource
        OracleQueryDebugInterceptor queryInterceptor = new OracleQueryDebugInterceptor(originalDataSource);
        listener.addListener(queryInterceptor);
        
        return ProxyDataSourceBuilder
                .create(originalDataSource)
                .name("QueryAnalyzerDS")
                .listener(listener)
                .build();
    }
}