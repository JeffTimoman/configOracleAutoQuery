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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.List;

@Component
public class OracleQueryInterceptor implements QueryExecutionListener {
    
    private static final Logger logger = LoggerFactory.getLogger(OracleQueryInterceptor.class);
    
    @Autowired
    private QueryAnalyzerProperties properties;
    
    @Autowired
    private ExplainPlanAnalyzer explainPlanAnalyzer;
    
    @Autowired
    private QueryFormatter queryFormatter;
    
    @Autowired
    private DataSource dataSource;
    
    @Override
    public void beforeQuery(ExecutionInfo execInfo, List<QueryInfo> queryInfoList) {
        if (!properties.isEnabled() || !"development".equals(properties.getMode())) {
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