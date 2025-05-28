package bca.oraclelog.queryanalyzer.interceptor;

import bca.oraclelog.queryanalyzer.config.QueryDebugProperties;
import bca.oraclelog.queryanalyzer.model.QueryExecutionSummary;
import bca.oraclelog.queryanalyzer.service.OracleQueryAnalyzer;
import bca.oraclelog.queryanalyzer.service.QueryDebugFormatter;
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
public class OracleQueryDebugInterceptor implements QueryExecutionListener, ApplicationContextAware {
    
    private static final Logger logger = LoggerFactory.getLogger(OracleQueryDebugInterceptor.class);
    
    private final DataSource dataSource;
    private ApplicationContext applicationContext;
    
    // Lazy-loaded dependencies
    private QueryDebugProperties properties;
    private OracleQueryAnalyzer queryAnalyzer;
    private QueryDebugFormatter formatter;
    
    // Default constructor for Spring component scanning
    public OracleQueryDebugInterceptor() {
        this.dataSource = null;
    }
    
    // Constructor for manual instantiation with DataSource
    public OracleQueryDebugInterceptor(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
    
    private void initializeDependencies() {
        if (properties == null && applicationContext != null) {
            try {
                properties = applicationContext.getBean(QueryDebugProperties.class);
                queryAnalyzer = applicationContext.getBean(OracleQueryAnalyzer.class);
                formatter = applicationContext.getBean(QueryDebugFormatter.class);
            } catch (Exception e) {
                logger.debug("Failed to initialize query debug dependencies: {}", e.getMessage());
            }
        }
    }
    
    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        initializeDependencies();
        
        // Check if debug is enabled
        if (properties == null || !properties.isEnabled()) {
            return;
        }
        
        for (QueryInfo queryInfo : queryInfoList) {
            String query = queryInfo.getQuery();
            if (isAnalyzableQuery(query)) {
                try {
                    debugQuery(query);
                } catch (Exception e) {
                    logger.debug("Failed to debug query: {}", e.getMessage());
                }
            }
        }
    }
    
    @Override
    public void afterQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        // Post-execution debugging if needed in the future
    }
    
    private boolean isAnalyzableQuery(String query) {
        String upperQuery = query.trim().toUpperCase();
        return upperQuery.startsWith("SELECT") || 
               upperQuery.startsWith("INSERT") || 
               upperQuery.startsWith("UPDATE") || 
               upperQuery.startsWith("DELETE");
    }
    
    private void debugQuery(String query) throws Exception {
        if (dataSource == null) {
            logger.debug("DataSource not available for query debugging");
            return;
        }
        
        try (Connection connection = dataSource.getConnection()) {
            QueryExecutionSummary summary = queryAnalyzer.analyzeQuery(connection, query);
            
            // Add query text if enabled
            if (properties.isIncludeQueryText()) {
                summary.setOriginalQuery(query);
            }
            
            // Add stack trace if enabled
            if (properties.isIncludeStackTrace()) {
                summary.setStackTrace(getCurrentStackTrace());
            }
            
            String debugOutput = formatter.formatQueryDebug(summary);
            
            // Output to console if enabled
            if (properties.isLogToConsole()) {
                System.out.println(debugOutput);
            }
            
            // Output to file if enabled
            if (properties.isLogToFile()) {
                logger.info(debugOutput);
            }
        }
    }
    
    private String getCurrentStackTrace() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        StringBuilder sb = new StringBuilder();
        
        // Skip internal methods and focus on application code
        for (int i = 4; i < Math.min(stackTrace.length, 8); i++) {
            StackTraceElement element = stackTrace[i];
            String className = element.getClassName();
            
            // Skip framework and proxy classes
            if (!className.contains("queryanalyzer") && 
                !className.contains("dsproxy") && 
                !className.contains("springframework.jdbc") &&
                !className.contains("hikari")) {
                sb.append("  at ").append(element.toString()).append("\n");
            }
        }
        
        return sb.toString();
    }
}