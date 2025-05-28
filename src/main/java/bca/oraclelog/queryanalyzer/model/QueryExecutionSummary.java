package bca.oraclelog.queryanalyzer.model;

import java.time.LocalDateTime;
import java.util.UUID;

public class QueryExecutionSummary {
    private String statementId;
    private String originalQuery;
    private double cost;
    private long cardinality;
    private long bytes;
    private double cpuCost;
    private double ioCost;
    private long time;
    private long analysisTimeMs;
    private LocalDateTime timestamp;
    private String stackTrace;
    
    public QueryExecutionSummary() {
        this.statementId = UUID.randomUUID().toString();
        this.timestamp = LocalDateTime.now();
    }
    
    // Getters and Setters
    public String getStatementId() { return statementId; }
    public void setStatementId(String statementId) { this.statementId = statementId; }
    
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }
    
    public double getCost() { return cost; }
    public void setCost(double cost) { this.cost = cost; }
    
    public long getCardinality() { return cardinality; }
    public void setCardinality(long cardinality) { this.cardinality = cardinality; }
    
    public long getBytes() { return bytes; }
    public void setBytes(long bytes) { this.bytes = bytes; }
    
    public double getCpuCost() { return cpuCost; }
    public void setCpuCost(double cpuCost) { this.cpuCost = cpuCost; }
    
    public double getIoCost() { return ioCost; }
    public void setIoCost(double ioCost) { this.ioCost = ioCost; }
    
    public long getTime() { return time; }
    public void setTime(long time) { this.time = time; }
    
    public long getAnalysisTimeMs() { return analysisTimeMs; }
    public void setAnalysisTimeMs(long analysisTimeMs) { this.analysisTimeMs = analysisTimeMs; }
    
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
    
    public String getStackTrace() { return stackTrace; }
    public void setStackTrace(String stackTrace) { this.stackTrace = stackTrace; }
}