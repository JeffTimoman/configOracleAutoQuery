package bca.oraclelog.queryanalyzer.model;

import java.util.List;

public class QueryExecutionPlan {
    private String originalQuery;
    private List<ExecutionStep> steps;
    private double totalCost;
    private long executionTime;
    private String stackTrace;
    
    public QueryExecutionPlan(String originalQuery) {
        this.originalQuery = originalQuery;
    }
    
    // Getters and Setters
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
    
    public List<ExecutionStep> getSteps() { return steps; }
    public void setSteps(List<ExecutionStep> steps) { this.steps = steps; }
    
    public double getTotalCost() { return totalCost; }
    public void setTotalCost(double totalCost) { this.totalCost = totalCost; }
    
    public long getExecutionTime() { return executionTime; }
    public void setExecutionTime(long executionTime) { this.executionTime = executionTime; }
    
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
    
    public static class ExecutionStep {
        private int id;
        private String operation;
        private String objectName;
        private double cost;
        private long cardinality;
        private String accessPredicates;
        private String filterPredicates;
        
        // Getters and Setters
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getOperation() { return operation; }
        public void setOperation(String operation) { this.operation = operation; }
        
        public String getObjectName() { return objectName; }
        public void setObjectName(String objectName) { this.objectName = objectName; }
        
        public double getCost() { return cost; }
        public void setCost(double cost) { this.cost = cost; }
        
        public long getCardinality() { return cardinality; }
        public void setCardinality(long cardinality) { this.cardinality = cardinality; }
        
        public String getAccessPredicates() { return accessPredicates; }
        public void setAccessPredicates(String accessPredicates) { this.accessPredicates = accessPredicates; }
        
        public String getFilterPredicates() { return filterPredicates; }
        public void setFilterPredicates(String filterPredicates) { this.filterPredicates = filterPredicates; }
    }
}