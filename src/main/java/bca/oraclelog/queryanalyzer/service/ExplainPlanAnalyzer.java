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
        String statementId = "STMT_" + System.currentTimeMillis() + "_" + Thread.currentThread().getId();
        
        try {
            // Execute EXPLAIN PLAN
            String explainQuery = String.format("EXPLAIN PLAN SET STATEMENT_ID = '%s' FOR %s", statementId, query);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute(explainQuery);
            }
            
            // Retrieve execution plan using DBMS_XPLAN
            List<QueryExecutionPlan.ExecutionStep> steps = retrieveExecutionPlanFromXPlan(connection, statementId);
            plan.setSteps(steps);
            
            // Calculate total cost
            double totalCost = steps.stream()
                .filter(step -> step.getCost() > 0)
                .mapToDouble(QueryExecutionPlan.ExecutionStep::getCost)
                .max()
                .orElse(0.0);
            plan.setTotalCost(totalCost);
            
        } finally {
            // Clean up plan table
            cleanupPlanTable(connection, statementId);
            plan.setExecutionTime(System.currentTimeMillis() - startTime);
        }
        
        return plan;
    }
    
    private List<QueryExecutionPlan.ExecutionStep> retrieveExecutionPlanFromXPlan(Connection connection, String statementId) throws Exception {
        List<QueryExecutionPlan.ExecutionStep> steps = new ArrayList<>();
        
        // First try to get the plan using DBMS_XPLAN.DISPLAY
        String xplanQuery = "SELECT * FROM TABLE(DBMS_XPLAN.DISPLAY('PLAN_TABLE', ?, 'BASIC +COST +BYTES +PREDICATE'))";
        
        try (PreparedStatement pstmt = connection.prepareStatement(xplanQuery)) {
            pstmt.setString(1, statementId);
            
            try (ResultSet rs = pstmt.executeQuery()) {
                boolean foundPlan = false;
                int currentId = 0;
                
                while (rs.next()) {
                    String planLine = rs.getString(1);
                    if (planLine == null) continue;
                    
                    // Parse the execution plan lines
                    if (planLine.contains("|") && !planLine.contains("---") && !planLine.contains("Id")) {
                        QueryExecutionPlan.ExecutionStep step = parsePlanLine(planLine);
                        if (step != null) {
                            steps.add(step);
                            foundPlan = true;
                        }
                    }
                }
                
                if (!foundPlan) {
                    // Fallback to direct PLAN_TABLE query
                    return retrieveExecutionPlanFromPlanTable(connection, statementId);
                }
            }
        }
        
        return steps;
    }
    
    private List<QueryExecutionPlan.ExecutionStep> retrieveExecutionPlanFromPlanTable(Connection connection, String statementId) throws Exception {
        List<QueryExecutionPlan.ExecutionStep> steps = new ArrayList<>();
        
        String planQuery = """
            SELECT ID, OPERATION, OBJECT_NAME, COST, CARDINALITY, BYTES,
                   ACCESS_PREDICATES, FILTER_PREDICATES, PROJECTION
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
                    
                    String operation = rs.getString("OPERATION");
                    String objectName = rs.getString("OBJECT_NAME");
                    if (objectName != null && !objectName.trim().isEmpty()) {
                        operation += " " + objectName;
                    }
                    step.setOperation(operation);
                    step.setObjectName(objectName);
                    
                    // Handle null values safely
                    Object cost = rs.getObject("COST");
                    step.setCost(cost != null ? ((Number) cost).doubleValue() : 0.0);
                    
                    Object cardinality = rs.getObject("CARDINALITY");
                    step.setCardinality(cardinality != null ? ((Number) cardinality).longValue() : 0L);
                    
                    step.setAccessPredicates(rs.getString("ACCESS_PREDICATES"));
                    step.setFilterPredicates(rs.getString("FILTER_PREDICATES"));
                    
                    steps.add(step);
                }
            }
        }
        
        return steps;
    }
    
    private QueryExecutionPlan.ExecutionStep parsePlanLine(String planLine) {
        try {
            // Parse format: | Id | Operation | Name | Rows | Bytes | Cost (%CPU) | Time |
            String[] parts = planLine.split("\\|");
            if (parts.length < 7) return null;
            
            QueryExecutionPlan.ExecutionStep step = new QueryExecutionPlan.ExecutionStep();
            
            // Parse ID
            String idStr = parts[1].trim();
            if (idStr.equals("*") || idStr.isEmpty()) return null;
            try {
                step.setId(Integer.parseInt(idStr.replaceAll("\\*", "").trim()));
            } catch (NumberFormatException e) {
                return null;
            }
            
            // Parse Operation
            step.setOperation(parts[2].trim());
            
            // Parse Object Name
            step.setObjectName(parts[3].trim().isEmpty() ? null : parts[3].trim());
            
            // Parse Cardinality (Rows)
            String rowsStr = parts[4].trim();
            if (!rowsStr.isEmpty()) {
                try {
                    step.setCardinality(parseNumericValue(rowsStr));
                } catch (Exception e) {
                    step.setCardinality(0L);
                }
            }
            
            // Parse Cost
            String costStr = parts[6].trim();
            if (!costStr.isEmpty()) {
                try {
                    // Extract cost from format like "1622 (1%)"
                    String[] costParts = costStr.split("\\s+");
                    if (costParts.length > 0) {
                        step.setCost(parseNumericValue(costParts[0]));
                    }
                } catch (Exception e) {
                    step.setCost(0.0);
                }
            }
            
            return step;
            
        } catch (Exception e) {
            return null;
        }
    }
    
    private long parseNumericValue(String value) {
        if (value == null || value.trim().isEmpty()) return 0L;
        
        value = value.trim().toUpperCase();
        
        // Handle K, M, G suffixes
        if (value.endsWith("K")) {
            return (long) (Double.parseDouble(value.substring(0, value.length() - 1)) * 1000);
        } else if (value.endsWith("M")) {
            return (long) (Double.parseDouble(value.substring(0, value.length() - 1)) * 1000000);
        } else if (value.endsWith("G")) {
            return (long) (Double.parseDouble(value.substring(0, value.length() - 1)) * 1000000000);
        } else {
            return Long.parseLong(value);
        }
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