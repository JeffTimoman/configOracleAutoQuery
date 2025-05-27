package bca.oraclelog.queryanalyzer.service;

import bca.oraclelog.queryanalyzer.model.QueryExecutionPlan;
import org.springframework.stereotype.Service;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExplainPlanAnalyzer {
    
    public QueryExecutionPlan analyzeQuery(Connection connection, String query) throws Exception {
        QueryExecutionPlan plan = new QueryExecutionPlan(query);
        
        long startTime = System.currentTimeMillis();
        
        // Generate unique statement ID
        String statementId = "STMT_" + System.currentTimeMillis();
        
        try {
            // Execute EXPLAIN PLAN
            String explainQuery = String.format("EXPLAIN PLAN SET STATEMENT_ID = '%s' FOR %s", statementId, query);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(explainQuery);
            }
            
            // Retrieve execution plan
            List<QueryExecutionPlan.ExecutionStep> steps = retrieveExecutionPlan(connection, statementId);
            plan.setSteps(steps);
            
            // Calculate total cost
            double totalCost = steps.stream().mapToDouble(QueryExecutionPlan.ExecutionStep::getCost).sum();
            plan.setTotalCost(totalCost);
            
        } finally {
            // Clean up plan table
            cleanupPlanTable(connection, statementId);
            plan.setExecutionTime(System.currentTimeMillis() - startTime);
        }
        
        return plan;
    }
    
    private List<QueryExecutionPlan.ExecutionStep> retrieveExecutionPlan(Connection connection, String statementId) throws Exception {
        List<QueryExecutionPlan.ExecutionStep> steps = new ArrayList<>();
        
        String planQuery = """
            SELECT ID, OPERATION, OBJECT_NAME, COST, CARDINALITY, 
                   ACCESS_PREDICATES, FILTER_PREDICATES
            FROM PLAN_TABLE 
            WHERE STATEMENT_ID = ? 
            ORDER BY ID
            """;
        
        try (PreparedStatement pstmt = connection.prepareStatement(planQuery)) {
            pstmt.setString(1, statementId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    QueryExecutionPlan.ExecutionStep step = new QueryExecutionPlan.ExecutionStep();
                    step.setId(rs.getInt("ID"));
                    step.setOperation(rs.getString("OPERATION"));
                    step.setObjectName(rs.getString("OBJECT_NAME"));
                    step.setCost(rs.getDouble("COST"));
                    step.setCardinality(rs.getLong("CARDINALITY"));
                    step.setAccessPredicates(rs.getString("ACCESS_PREDICATES"));
                    step.setFilterPredicates(rs.getString("FILTER_PREDICATES"));
                    
                    steps.add(step);
                }
            }
        }
        
        return steps;
    }
    
    private void cleanupPlanTable(Connection connection, String statementId) {
        try (PreparedStatement pstmt = connection.prepareStatement("DELETE FROM PLAN_TABLE WHERE STATEMENT_ID = ?")) {
            pstmt.setString(1, statementId);
            pstmt.execute();
        } catch (Exception e) {
            // Log warning but don't fail
            System.err.println("Warning: Failed to cleanup plan table: " + e.getMessage());
        }
    }
}