package bca.oraclelog.queryanalyzer.service;

import bca.oraclelog.queryanalyzer.model.QueryExecutionSummary;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

@Service
public class OracleQueryAnalyzer {
    
    public QueryExecutionSummary analyzeQuery(Connection connection, String query) throws Exception {
        QueryExecutionSummary summary = new QueryExecutionSummary();
        summary.setOriginalQuery(query);
        
        long startTime = System.currentTimeMillis();
        String statementId = summary.getStatementId();
        
        try {
            // Execute EXPLAIN PLAN
            String explainQuery = String.format("EXPLAIN PLAN SET STATEMENT_ID = '%s' FOR %s", statementId, query);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(explainQuery);
            }
            
            // Get parent execution plan details (ID = 0)
            retrieveParentPlanDetails(connection, statementId, summary);
            
        } finally {
            // Clean up plan table
            cleanupPlanTable(connection, statementId);
            summary.setAnalysisTimeMs(System.currentTimeMillis() - startTime);
        }
        
        return summary;
    }
    
    private void retrieveParentPlanDetails(Connection connection, String statementId, QueryExecutionSummary summary) throws Exception {
        String planQuery = """
            SELECT COST, CARDINALITY, BYTES, CPU_COST, IO_COST, TIME
            FROM PLAN_TABLE 
            WHERE STATEMENT_ID = ? AND ID = 0
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(planQuery)) {
            pstmt.setString(1, statementId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Handle null values safely
                    Object cost = rs.getObject("COST");
                    summary.setCost(cost != null ? ((Number) cost).doubleValue() : 0.0);
                    
                    Object cardinality = rs.getObject("CARDINALITY");
                    summary.setCardinality(cardinality != null ? ((Number) cardinality).longValue() : 0L);
                    
                    Object bytes = rs.getObject("BYTES");
                    summary.setBytes(bytes != null ? ((Number) bytes).longValue() : 0L);
                    
                    Object cpuCost = rs.getObject("CPU_COST");
                    summary.setCpuCost(cpuCost != null ? ((Number) cpuCost).doubleValue() : 0.0);
                    
                    Object ioCost = rs.getObject("IO_COST");
                    summary.setIoCost(ioCost != null ? ((Number) ioCost).doubleValue() : 0.0);
                    
                    Object time = rs.getObject("TIME");
                    summary.setTime(time != null ? ((Number) time).longValue() : 0L);
                }
            }
        }
    }
    
    private void cleanupPlanTable(Connection connection, String statementId) {
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM PLAN_TABLE WHERE STATEMENT_ID = ?")) {
            pstmt.setString(1, statementId);
            pstmt.execute();
        } catch (Exception e) {
            // Silently ignore cleanup errors
        }
    }
}