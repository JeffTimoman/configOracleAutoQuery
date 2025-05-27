package bca.oraclelog.queryanalyzer.interceptor;

import bca.oraclelog.queryanalyzer.config.QueryAnalyzerProperties;
import bca.oraclelog.queryanalyzer.model.QueryExecutionPlan;
import bca.oraclelog.queryanalyzer.service.ExplainPlanAnalyzer;
import bca.oraclelog.queryanalyzer.service.QueryFormatter;
import net.ttddyy.dsproxy.ExecutionInfo;
import net.ttddyy.dsproxy.QueryInfo;
import net.ttddyy.dsproxy.listener.QueryExecutionListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Component
public class OracleQueryInterceptor implements QueryExecutionListener, ApplicationContextAware {
    
    private static final Logger logger = LoggerFactory.getLogger(OracleQueryInterceptor.class);
    
    private final DataSource dataSource;
    private ApplicationContext applicationContext;
    
    // Lazy-loaded dependencies
    private QueryAnalyzerProperties properties;
    private ExplainPlanAnalyzer explainPlanAnalyzer;
    private QueryFormatter queryFormatter;
    
    // Default constructor for Spring component scanning
    public OracleQueryInterceptor() {
        this.dataSource = null;
    }
    
    // Constructor for manual instantiation with DataSource
    public OracleQueryInterceptor(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    private void initializeDependencies() {
        if (properties == null && applicationContext != null) {
            try {
                properties = applicationContext.getBean(QueryAnalyzerProperties.class);
                explainPlanAnalyzer = applicationContext.getBean(ExplainPlanAnalyzer.class);
                queryFormatter = applicationContext.getBean(QueryFormatter.class);
            } catch (Exception e) {
                logger.warn("Failed to initialize dependencies: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        initializeDependencies();
        
        if (properties == null || !properties.isEnabled() || !"development".equals(properties.getMode())) {
            return;
        }
        
        for (QueryInfo queryInfo : queryInfoList) {
            String query = queryInfo.getQuery();
            if (isAnalyzableQuery(query)) {
                try {
                    analyzeQuery(query);
                } catch (Exception e) {
                    logger.warn("Failed to analyze query: {}", e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        // Implementation for post-execution analysis if needed
    }
    
    private boolean isAnalyzableQuery(String query) {
        String upperQuery = query.trim().toUpperCase();
        return upperQuery.startsWith("SELECT") || 
               upperQuery.startsWith("INSERT") || 
               upperQuery.startsWith("UPDATE") || 
               upperQuery.startsWith("DELETE");
    }
    
    private void analyzeQuery(String query) throws Exception {
        if (dataSource == null) {
            logger.warn("DataSource not available for query analysis");
            return;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            QueryExecutionPlan plan = explainPlanAnalyzer.analyzeQuery(connection, query);
            
            if (properties.isIncludeStackTrace()) {
                plan.setStackTrace(getCurrentStackTrace());
            }
            
            String formattedOutput = queryFormatter.formatAnalysis(plan);
            
            if (properties.isLogToConsole()) {
                System.out.println(formattedOutput);
            }
            
            if (properties.isLogToFile()) {
                // Log to file implementation
                logger.info(formattedOutput);
            }
        }
    }
    
    private String getCurrentStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        // Skip first few elements (getStackTrace, getCurrentStackTrace, analyzeQuery)
        for (int i = 4; i < Math.min(stackTrace.length, 10); i++) {
            StackTraceElement element = stackTrace[i];
            if (!element.getClassName().contains("queryanalyzer")) {
                sb.append("  at ").append(element.toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
}