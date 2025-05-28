package bca.oraclelog.queryanalyzer.service;

import bca.oraclelog.queryanalyzer.model.QueryExecutionSummary;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;

@Service
public class QueryDebugFormatter {
    
    private static final String SEPARATOR = "=".repeat(100);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    public String formatQueryDebug(QueryExecutionSummary summary) {
        StringBuilder sb = new StringBuilder();
        
        // Header
        sb.append("\n").append(SEPARATOR).append("\n");
        sb.append("🔍 ORACLE QUERY DEBUG - ").append(summary.getTimestamp().format(TIMESTAMP_FORMAT)).append("\n");
        sb.append("📋 Statement ID: ").append(summary.getStatementId()).append("\n");
        sb.append("⏱️  Analysis Time: ").append(summary.getAnalysisTimeMs()).append(" ms\n");
        sb.append(SEPARATOR).append("\n");
        
        // Query (if enabled)
        if (summary.getOriginalQuery() != null) {
            sb.append("📝 SQL QUERY:\n");
            sb.append(formatQuery(summary.getOriginalQuery())).append("\n\n");
        }
        
        // Execution Plan Summary (ID=0 only)
        sb.append("📊 EXECUTION PLAN SUMMARY (Parent Node):\n");
        sb.append("-".repeat(60)).append("\n");
        sb.append(String.format("💰 Total Cost:      %,.0f\n", summary.getCost()));
        sb.append(String.format("📈 Cardinality:     %,d rows\n", summary.getCardinality()));
        sb.append(String.format("💾 Bytes:           %,d bytes (%s)\n", summary.getBytes(), formatBytes(summary.getBytes())));
        sb.append(String.format("🖥️  CPU Cost:        %,.0f\n", summary.getCpuCost()));
        sb.append(String.format("💿 I/O Cost:        %,.0f\n", summary.getIoCost()));
        sb.append(String.format("⏰ Estimated Time:  %,d microseconds\n", summary.getTime()));
        
        // Performance Assessment
        sb.append("\n").append(generatePerformanceAssessment(summary)).append("\n");
        
        // Stack Trace (if available)
        if (summary.getStackTrace() != null && !summary.getStackTrace().isEmpty()) {
            sb.append("📍 CALL STACK:\n");
            sb.append("-".repeat(60)).append("\n");
            sb.append(summary.getStackTrace()).append("\n");
        }
        
        sb.append(SEPARATOR).append("\n");
        
        return sb.toString();
    }
    
    private String formatQuery(String query) {
        return query.trim()
                   .replaceAll("(?i)\\b(SELECT|FROM|WHERE|ORDER BY|GROUP BY|HAVING|JOIN|LEFT JOIN|RIGHT JOIN|INNER JOIN)\\b", "\n$1")
                   .replaceAll("\\s+", " ")
                   .trim();
    }
    
    private String formatBytes(long bytes) {
        if (bytes == 0) return "0 B";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int digitGroups = (int) (Math.log10(bytes) / Math.log10(1024));
        
        return String.format("%.1f %s", 
            bytes / Math.pow(1024, digitGroups), 
            units[digitGroups]);
    }
    
    private String generatePerformanceAssessment(QueryExecutionSummary summary) {
        StringBuilder assessment = new StringBuilder();
        assessment.append("🎯 PERFORMANCE ASSESSMENT:\n");
        assessment.append("-".repeat(60)).append("\n");
        
        // Cost assessment
        if (summary.getCost() > 10000) {
            assessment.append("🔴 HIGH COST: Query cost is very high (").append(String.format("%.0f", summary.getCost())).append(")\n");
            assessment.append("   → Consider optimizing with indexes or query rewrite\n");
        } else if (summary.getCost() > 1000) {
            assessment.append("🟡 MODERATE COST: Query cost is moderate (").append(String.format("%.0f", summary.getCost())).append(")\n");
            assessment.append("   → Monitor performance in production\n");
        } else {
            assessment.append("🟢 LOW COST: Query cost is acceptable (").append(String.format("%.0f", summary.getCost())).append(")\n");
        }
        
        // Cardinality assessment
        if (summary.getCardinality() > 1000000) {
            assessment.append("🔴 HIGH CARDINALITY: Processing ").append(String.format("%,d", summary.getCardinality())).append(" rows\n");
            assessment.append("   → Consider adding WHERE clauses to reduce result set\n");
        } else if (summary.getCardinality() > 10000) {
            assessment.append("🟡 MODERATE CARDINALITY: Processing ").append(String.format("%,d", summary.getCardinality())).append(" rows\n");
        }
        
        // I/O vs CPU ratio
        if (summary.getIoCost() > 0 && summary.getCpuCost() > 0) {
            double ioRatio = summary.getIoCost() / (summary.getIoCost() + summary.getCpuCost());
            if (ioRatio > 0.8) {
                assessment.append("💿 I/O INTENSIVE: Query is I/O bound (").append(String.format("%.1f%%", ioRatio * 100)).append(" I/O)\n");
                assessment.append("   → Consider adding indexes to reduce I/O\n");
            } else if (ioRatio < 0.2) {
                assessment.append("🖥️  CPU INTENSIVE: Query is CPU bound (").append(String.format("%.1f%%", (1-ioRatio) * 100)).append(" CPU)\n");
                assessment.append("   → Consider optimizing complex calculations\n");
            }
        }
        
        return assessment.toString();
    }
}