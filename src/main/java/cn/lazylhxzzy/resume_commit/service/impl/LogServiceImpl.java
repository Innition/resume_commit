package cn.lazylhxzzy.resume_commit.service.impl;

import cn.lazylhxzzy.resume_commit.entity.AccessLog;
import cn.lazylhxzzy.resume_commit.entity.SecurityLog;
import cn.lazylhxzzy.resume_commit.entity.SystemLog;
import cn.lazylhxzzy.resume_commit.mapper.AccessLogMapper;
import cn.lazylhxzzy.resume_commit.mapper.SecurityLogMapper;
import cn.lazylhxzzy.resume_commit.mapper.SystemLogMapper;
import cn.lazylhxzzy.resume_commit.service.LogService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * 日志服务实现类
 */
@Service
public class LogServiceImpl implements LogService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogServiceImpl.class);
    private static final Logger accessLogger = LoggerFactory.getLogger("ACCESS_LOG");
    private static final Logger securityLogger = LoggerFactory.getLogger("SECURITY_LOG");
    private static final Logger performanceLogger = LoggerFactory.getLogger("PERFORMANCE_LOG");
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    @Autowired
    private AccessLogMapper accessLogMapper;
    
    @Autowired
    private SecurityLogMapper securityLogMapper;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    @Override
    public void logSystem(String level, String type, String module, String operation,
                         Long userId, String username, String ipAddress, String userAgent,
                         String requestMethod, String requestUrl, String requestParams,
                         Integer responseCode, Long responseTime, String errorCode,
                         String errorMessage, String stackTrace, Map<String, Object> extraData) {
        try {
            SystemLog systemLog = new SystemLog();
            systemLog.setLogLevel(level);
            systemLog.setLogType(type);
            systemLog.setModule(module);
            systemLog.setOperation(operation);
            systemLog.setUserId(userId);
            systemLog.setUsername(username);
            systemLog.setIpAddress(ipAddress);
            systemLog.setUserAgent(userAgent);
            systemLog.setRequestMethod(requestMethod);
            systemLog.setRequestUrl(requestUrl);
            systemLog.setRequestParams(requestParams);
            systemLog.setResponseCode(responseCode);
            systemLog.setResponseTime(responseTime);
            systemLog.setErrorCode(errorCode);
            systemLog.setErrorMessage(errorMessage);
            systemLog.setStackTrace(stackTrace);
            systemLog.setExtraData(convertToJson(extraData));
            systemLog.setCreatedAt(LocalDateTime.now());
            
            systemLogMapper.insert(systemLog);
            
            // 同时写入文件日志
            logger.info("系统日志记录成功: {} - {} - {}", module, operation, errorMessage);
            
        } catch (Exception e) {
            logger.error("记录系统日志失败", e);
        }
    }
    
    @Override
    public void logAccess(Long userId, String username, String ipAddress, String userAgent,
                         String requestMethod, String requestUrl, String requestParams,
                         Integer responseCode, Long responseTime, Long requestSize,
                         Long responseSize, String referer, String sessionId) {
        try {
            AccessLog accessLog = new AccessLog();
            accessLog.setUserId(userId);
            accessLog.setUsername(username);
            accessLog.setIpAddress(ipAddress);
            accessLog.setUserAgent(userAgent);
            accessLog.setRequestMethod(requestMethod);
            accessLog.setRequestUrl(requestUrl);
            accessLog.setRequestParams(requestParams);
            accessLog.setResponseCode(responseCode);
            accessLog.setResponseTime(responseTime);
            accessLog.setRequestSize(requestSize);
            accessLog.setResponseSize(responseSize);
            accessLog.setReferer(referer);
            accessLog.setSessionId(sessionId);
            accessLog.setCreatedAt(LocalDateTime.now());
            
            accessLogMapper.insert(accessLog);
            
            // 同时写入文件日志
            accessLogger.info("访问日志记录成功: {} {} - {} - {}ms", requestMethod, requestUrl, responseCode, responseTime);
            
        } catch (Exception e) {
            logger.error("记录访问日志失败", e);
        }
    }
    
    @Override
    public void logSecurity(String logType, Long userId, String username, String ipAddress,
                           String userAgent, String eventDescription, String riskLevel,
                           Map<String, Object> additionalData) {
        try {
            SecurityLog securityLog = new SecurityLog();
            securityLog.setLogType(logType);
            securityLog.setUserId(userId);
            securityLog.setUsername(username);
            securityLog.setIpAddress(ipAddress);
            securityLog.setUserAgent(userAgent);
            securityLog.setEventDescription(eventDescription);
            securityLog.setRiskLevel(riskLevel);
            securityLog.setAdditionalData(convertToJson(additionalData));
            securityLog.setCreatedAt(LocalDateTime.now());
            
            securityLogMapper.insert(securityLog);
            
            // 同时写入文件日志
            securityLogger.warn("安全日志记录成功: {} - {} - {}", logType, eventDescription, riskLevel);
            
        } catch (Exception e) {
            logger.error("记录安全日志失败", e);
        }
    }
    
    @Override
    public void logBusiness(String operation, Long userId, String username, String ipAddress,
                           Map<String, Object> businessData) {
        logSystem("INFO", "BUSINESS", "BUSINESS", operation, userId, username, ipAddress, null,
                 null, null, null, null, null, null, null, null, businessData);
    }
    
    @Override
    public void logError(String module, String operation, Long userId, String username,
                        String ipAddress, String errorCode, String errorMessage,
                        String stackTrace, Map<String, Object> extraData) {
        logSystem("ERROR", "ERROR", module, operation, userId, username, ipAddress, null,
                 null, null, null, null, null, errorCode, errorMessage, stackTrace, extraData);
    }
    
    @Override
    public void logPerformance(String module, String methodName, Long executionTime,
                              Long memoryUsage, Double cpuUsage, Map<String, Object> metrics) {
        try {
            // 记录到性能日志文件
            performanceLogger.info("性能监控: {} - {} - {}ms - {}MB - {}%", 
                                 module, methodName, executionTime, 
                                 memoryUsage != null ? memoryUsage / 1024 / 1024 : 0, 
                                 cpuUsage != null ? cpuUsage : 0);
            
            // 如果执行时间超过阈值，记录到数据库
            if (executionTime > 1000) { // 超过1秒
                logSystem("WARN", "PERFORMANCE", module, methodName, null, null, null, null,
                         null, null, null, null, executionTime, null, null, null, metrics);
            }
            
        } catch (Exception e) {
            logger.error("记录性能日志失败", e);
        }
    }
    
    @Override
    @Async
    public void logSystemAsync(String level, String type, String module, String operation,
                              Long userId, String username, String ipAddress, String userAgent,
                              String requestMethod, String requestUrl, String requestParams,
                              Integer responseCode, Long responseTime, String errorCode,
                              String errorMessage, String stackTrace, Map<String, Object> extraData) {
        logSystem(level, type, module, operation, userId, username, ipAddress, userAgent,
                 requestMethod, requestUrl, requestParams, responseCode, responseTime,
                 errorCode, errorMessage, stackTrace, extraData);
    }
    
    @Override
    @Async
    public void logAccessAsync(Long userId, String username, String ipAddress, String userAgent,
                              String requestMethod, String requestUrl, String requestParams,
                              Integer responseCode, Long responseTime, Long requestSize,
                              Long responseSize, String referer, String sessionId) {
        logAccess(userId, username, ipAddress, userAgent, requestMethod, requestUrl, requestParams,
                 responseCode, responseTime, requestSize, responseSize, referer, sessionId);
    }
    
    /**
     * 将Map转换为JSON字符串
     */
    private String convertToJson(Map<String, Object> data) {
        if (data == null || data.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(data);
        } catch (JsonProcessingException e) {
            logger.error("转换JSON失败", e);
            return null;
        }
    }
}
