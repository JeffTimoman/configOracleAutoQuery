package bca.oraclelog.queryanalyzer.service;

import bca.oraclelog.queryanalyzer.model.QueryExecutionPlan;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class QueryFormatter {
    
    private static final String SEPARATOR = "=".repeat(80);
    private static final String LINE = "-".repeat(80);
    
    public String formatAnalysis(QueryExecutionPlan plan) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("\n").append(SEPARATOR).append("\n");
        sb.append("🔍 ORACLE QUERY ANALYSIS REPORT\n");
        sb.append("📅 Timestamp: ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))).append("\n");
        sb.append("⏱️  Analysis Time: ").append(plan.getExecutionTime()).append(" ms\n");
        sb.append("💰 Total Cost: ").append(String.format("%.2f", plan.getTotalCost())).append("\n");
        sb.append(SEPARATOR).append("\n\n");
        
        // Original Query
        sb.append("📝 ORIGINAL QUERY:\n");
        sb.append(LINE).append("\n");
        sb.append(formatQuery(plan.getOriginalQuery())).append("\n\n");
        
        // Execution Plan
        sb.append("🏗️  EXECUTION PLAN:\n");
        sb.append(LINE).append("\n");
        sb.append(formatExecutionPlan(plan)).append("\n");
        
        // Stack Trace (if available)
        if (plan.getStackTrace() != null && !plan.getStackTrace().isEmpty()) {
            sb.append("📍 CALL STACK:\n");
            sb.append(LINE).append("\n");
            sb.append(plan.getStackTrace()).append("\n");
        }
        
        // Performance Recommendations
        sb.append("💡 RECOMMENDATIONS:\n");
        sb.append(LINE).append("\n");
        sb.append(generateRecommendations(plan)).append("\n");
        
        sb.append(SEPARATOR).append("\n");
        
        return sb.toString();
    }
    
    private String formatQuery(String query) {
        // Simple query formatting
        return query.trim()
                   .replaceAll("(?i)\\b(SELECT|FROM|WHERE|ORDER BY|GROUP BY|HAVING|JOIN|LEFT JOIN|RIGHT JOIN|INNER JOIN)\\b", "\n$1")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    private String formatExecutionPlan(QueryExecutionPlan plan) {
        StringBuilder sb = new StringBuilder();
        
        sb.append(String.format("%-4s %-25s %-20s %-10s %-12s%n", 
                "ID", "OPERATION", "OBJECT", "COST", "CARDINALITY"));
        sb.append("-".repeat(75)).append("\n");
        
        for (QueryExecutionPlan.ExecutionStep step : plan.getSteps()) {
            sb.append(String.format("%-4d %-25s %-20s %-10.0f %-12d%n",
                    step.getId(),
                    truncate(step.getOperation(), 25),
                    truncate(step.getObjectName(), 20),
                    step.getCost(),
                    step.getCardinality()));
            
            if (step.getAccessPredicates() != null) {
                sb.append("     Access: ").append(step.getAccessPredicates()).append("\n");
            }
            if (step.getFilterPredicates() != null) {
                sb.append("     Filter: ").append(step.getFilterPredicates()).append("\n");
            }
        }
        
        return sb.toString();
    }
    
    private String generateRecommendations(QueryExecutionPlan plan) {
        StringBuilder recommendations = new StringBuilder();
        
        // Cost-based recommendations
        if (plan.getTotalCost() > 1000) {
            recommendations.append("⚠️  High query cost detected (").append(String.format("%.0f", plan.getTotalCost())).append(")\n");
            recommendations.append("   - Consider adding indexes on frequently filtered columns\n");
            recommendations.append("   - Review WHERE clause conditions\n");
        }
        
        // Operation-based recommendations
        for (QueryExecutionPlan.ExecutionStep step : plan.getSteps()) {
            if (step.getOperation().contains("TABLE ACCESS FULL")) {
                recommendations.append("🔍 Full table scan detected on: ").append(step.getObjectName()).append("\n");
                recommendations.append("   - Consider adding appropriate indexes\n");
            }
            
            if (step.getOperation().contains("SORT") && step.getCost() > 100) {
                recommendations.append("📊 Expensive sort operation detected\n");
                recommendations.append("   - Consider adding indexes to avoid sorting\n");
            }
        }
        
        if (recommendations.length() == 0) {
            recommendations.append("✅ No major performance issues detected\n");
        }
        
        return recommendations.toString();
    }
    
    private String truncate(String str, int length) {
        if (str == null) return "";
        return str.length() > length ? str.substring(0, length - 3) + "..." : str;
    }
}