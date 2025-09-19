package cn.lazylhxzzy.resume_commit.service;

import java.util.Map;

/**
 * 日志服务接口
 */
public interface LogService {
    
    /**
     * 记录系统日志
     */
    void logSystem(String level, String type, String module, String operation, 
                   Long userId, String username, String ipAddress, String userAgent,
                   String requestMethod, String requestUrl, String requestParams,
                   Integer responseCode, Long responseTime, String errorCode,
                   String errorMessage, String stackTrace, Map<String, Object> extraData);
    
    /**
     * 记录访问日志
     */
    void logAccess(Long userId, String username, String ipAddress, String userAgent,
                   String requestMethod, String requestUrl, String requestParams,
                   Integer responseCode, Long responseTime, Long requestSize,
                   Long responseSize, String referer, String sessionId);
    
    /**
     * 记录安全日志
     */
    void logSecurity(String logType, Long userId, String username, String ipAddress,
                     String userAgent, String eventDescription, String riskLevel,
                     Map<String, Object> additionalData);
    
    /**
     * 记录业务日志
     */
    void logBusiness(String operation, Long userId, String username, String ipAddress,
                     Map<String, Object> businessData);
    
    /**
     * 记录错误日志
     */
    void logError(String module, String operation, Long userId, String username,
                  String ipAddress, String errorCode, String errorMessage,
                  String stackTrace, Map<String, Object> extraData);
    
    /**
     * 记录性能日志
     */
    void logPerformance(String module, String methodName, Long executionTime,
                        Long memoryUsage, Double cpuUsage, Map<String, Object> metrics);
    
    /**
     * 异步记录系统日志
     */
    void logSystemAsync(String level, String type, String module, String operation,
                        Long userId, String username, String ipAddress, String userAgent,
                        String requestMethod, String requestUrl, String requestParams,
                        Integer responseCode, Long responseTime, String errorCode,
                        String errorMessage, String stackTrace, Map<String, Object> extraData);
    
    /**
     * 异步记录访问日志
     */
    void logAccessAsync(Long userId, String username, String ipAddress, String userAgent,
                        String requestMethod, String requestUrl, String requestParams,
                        Integer responseCode, Long responseTime, Long requestSize,
                        Long responseSize, String referer, String sessionId);
}
