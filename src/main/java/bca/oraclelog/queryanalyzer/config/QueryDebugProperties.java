package bca.oraclelog.queryanalyzer.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "oracle.query.debug")
public class QueryDebugProperties {
    
    private boolean enabled = false;
    private boolean logToConsole = true;
    private boolean logToFile = false;
    private String logFilePath = "oracle-query-debug.log";
    private boolean includeStackTrace = false;
    private boolean includeQueryText = true;
    
    // Getters and Setters
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    
    public boolean isLogToConsole() { return logToConsole; }
    public void setLogToConsole(boolean logToConsole) { this.logToConsole = logToConsole; }
    
    public boolean isLogToFile() { return logToFile; }
    public void setLogToFile(boolean logToFile) { this.logToFile = logToFile; }
    
    public String getLogFilePath() { return logFilePath; }
    public void setLogFilePath(String logFilePath) { this.logFilePath = logFilePath; }
    
    public boolean isIncludeStackTrace() { return includeStackTrace; }
    public void setIncludeStackTrace(boolean includeStackTrace) { this.includeStackTrace = includeStackTrace; }
    
    public boolean isIncludeQueryText() { return includeQueryText; }
    public void setIncludeQueryText(boolean includeQueryText) { this.includeQueryText = includeQueryText; }
}