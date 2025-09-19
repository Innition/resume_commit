package cn.lazylhxzzy.resume_commit.util;

import cn.lazylhxzzy.resume_commit.entity.User;
import cn.lazylhxzzy.resume_commit.mapper.UserMapper;
import cn.lazylhxzzy.resume_commit.service.LogService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 日志工具类
 */
@Component
public class LogUtil {
    
    @Autowired
    private LogService logService;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Autowired
    private UserMapper userMapper;
    
    /**
     * 记录INFO级别系统日志
     */
    public void info(String module, String operation, String message) {
        logService.logSystem("INFO", "SYSTEM", module, operation, 
                           getCurrentUserId(), getCurrentUsername(), getCurrentIp(), getUserAgent(),
                           null, null, null, null, null, null, message, null, null);
    }
    
    /**
     * 记录WARN级别系统日志
     */
    public void warn(String module, String operation, String message) {
        logService.logSystem("WARN", "SYSTEM", module, operation,
                           getCurrentUserId(), getCurrentUsername(), getCurrentIp(), getUserAgent(),
                           null, null, null, null, null, null, message, null, null);
    }
    
    /**
     * 记录ERROR级别系统日志
     */
    public void error(String module, String operation, String errorCode, String errorMessage, String stackTrace) {
        logService.logError(module, operation, getCurrentUserId(), getCurrentUsername(),
                          getCurrentIp(), errorCode, errorMessage, stackTrace, null);
    }
    
    /**
     * 记录业务日志
     */
    public void business(String operation, String message, Map<String, Object> businessData) {
        logService.logBusiness(operation, getCurrentUserId(), getCurrentUsername(),
                             getCurrentIp(), businessData);
    }
    
    /**
     * 记录安全日志
     */
    public void security(String logType, String eventDescription, String riskLevel) {
        logService.logSecurity(logType, getCurrentUserId(), getCurrentUsername(),
                             getCurrentIp(), getUserAgent(), eventDescription, riskLevel, null);
    }
    
    /**
     * 记录性能日志
     */
    public void performance(String module, String methodName, Long executionTime, Map<String, Object> metrics) {
        logService.logPerformance(module, methodName, executionTime, null, null, metrics);
    }
    
    /**
     * 记录访问日志
     */
    public void logAccess(Long userId, String username, String ipAddress, String userAgent,
                         String requestMethod, String requestUrl, String requestParams,
                         Integer responseCode, Long responseTime, Long requestSize,
                         Long responseSize, String referer, String sessionId) {
        logService.logAccess(userId, username, ipAddress, userAgent, requestMethod, requestUrl,
                           requestParams, responseCode, responseTime, requestSize, responseSize,
                           referer, sessionId);
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            String username = getCurrentUsername();
            if (username != null) {
                User user = userMapper.selectOne(new QueryWrapper<User>().eq("username", username));
                return user != null ? user.getId() : null;
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        return null;
    }
    
    /**
     * 获取当前用户名
     */
    private String getCurrentUsername() {
        try {
            HttpServletRequest request = getCurrentRequest();
            if (request != null) {
                String token = getTokenFromRequest(request);
                if (token != null) {
                    return jwtUtil.getUsernameFromToken(token);
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        return null;
    }
    
    /**
     * 获取当前请求对象
     */
    private HttpServletRequest getCurrentRequest() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            return attributes != null ? attributes.getRequest() : null;
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * 从请求中获取JWT Token
     */
    private String getTokenFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return null;
    }
    
    /**
     * 获取当前IP地址
     */
    private String getCurrentIp() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return getClientIpAddress(request);
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return "unknown";
    }
    
    /**
     * 获取用户代理
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                HttpServletRequest request = attributes.getRequest();
                return request.getHeader("User-Agent");
            }
        } catch (Exception e) {
            // 忽略异常
        }
        return "unknown";
    }
    
    /**
     * 获取客户端真实IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        String xForwardedFor = request.getHeader("X-Forwarded-For");
        if (xForwardedFor != null && !xForwardedFor.isEmpty() && !"unknown".equalsIgnoreCase(xForwardedFor)) {
            return xForwardedFor.split(",")[0].trim();
        }
        
        String xRealIp = request.getHeader("X-Real-IP");
        if (xRealIp != null && !xRealIp.isEmpty() && !"unknown".equalsIgnoreCase(xRealIp)) {
            return xRealIp;
        }
        
        return request.getRemoteAddr();
    }
}
