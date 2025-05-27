package bca.oraclelog.queryanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "query.analyzer")
public class QueryAnalyzerProperties {
    
    private boolean enabled = false;
    private String mode = "development"; // development, production
    private boolean logToConsole = true;
    private boolean logToFile = false;
    private String logFilePath = "query-analysis.log";
    private boolean includeStackTrace = true;
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public String getMode() { return mode; }
    public void setMode(String mode) { this.mode = mode; }
    
    public boolean isLogToConsole() { return logToConsole; }
    public void setLogToConsole(boolean logToConsole) { this.logToConsole = logToConsole; }
    
    public boolean isLogToFile() { return logToFile; }
    public void setLogToFile(boolean logToFile) { this.logToFile = logToFile; }
    
    public String getLogFilePath() { return logFilePath; }
    public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }
    
    public boolean isIncludeStackTrace() { return includeStackTrace; }
    public void setIncludeStackTrace(boolean includeStackTrace) { this.includeStackTrace = includeStackTrace; }
}