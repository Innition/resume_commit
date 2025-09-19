package cn.lazylhxzzy.resume_commit.aspect;

import cn.lazylhxzzy.resume_commit.util.LogUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import jakarta.servlet.http.HttpServletRequest;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 日志记录AOP切面
 */
@Aspect
@Component
public class LoggingAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(LoggingAspect.class);
    
    @Autowired
    private LogUtil logUtil;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    /**
     * 环绕通知：记录API调用日志
     */
    @Around("execution(* cn.lazylhxzzy.resume_commit.controller.*.*(..))")
    public Object logApiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String module = className.replace("Controller", "");
        
        // 获取请求信息
        HttpServletRequest request = getCurrentRequest();
        String requestMethod = request != null ? request.getMethod() : "UNKNOWN";
        String requestUrl = request != null ? request.getRequestURI() : "UNKNOWN";
        String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";
        String ipAddress = getClientIpAddress(request);
        
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        String requestParams = serializeParameters(args);
        
        Object result = null;
        String errorMessage = null;
        String stackTrace = null;
        Integer responseCode = 200;
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录成功日志
            long executionTime = System.currentTimeMillis() - startTime;
            logUtil.info(module, methodName, "API调用成功");
            
            // 记录访问日志
            logUtil.logAccess(getCurrentUserId(), getCurrentUsername(), ipAddress, userAgent,
                            requestMethod, requestUrl, requestParams, 200, executionTime,
                            null, null, null, null);
            
            // 记录性能日志
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("execution_time", executionTime);
            metrics.put("request_method", requestMethod);
            metrics.put("request_url", requestUrl);
            logUtil.performance(module, methodName, executionTime, metrics);
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            errorMessage = e.getMessage();
            stackTrace = getStackTrace(e);
            responseCode = 500;
            
            // 记录访问日志（错误情况）
            logUtil.logAccess(getCurrentUserId(), getCurrentUsername(), ipAddress, userAgent,
                            requestMethod, requestUrl, requestParams, responseCode, executionTime,
                            null, null, null, null);
            
            logUtil.error(module, methodName, "API_ERROR_001", errorMessage, stackTrace);
            
            // 重新抛出异常
            throw e;
        }
        
        return result;
    }
    
    /**
     * 环绕通知：记录业务操作日志
     */
    @Around("@annotation(cn.lazylhxzzy.resume_commit.annotation.BusinessLog)")
    public Object logBusinessOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String module = className.replace("ServiceImpl", "");
        
        // 获取方法参数
        Object[] args = joinPoint.getArgs();
        Map<String, Object> businessData = new HashMap<>();
        businessData.put("method", methodName);
        businessData.put("class", className);
        businessData.put("parameters", serializeParameters(args));
        
        Object result = null;
        String errorMessage = null;
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录成功日志
            long executionTime = System.currentTimeMillis() - startTime;
            businessData.put("execution_time", executionTime);
            businessData.put("result", "SUCCESS");
            
            logUtil.business(methodName, "业务操作成功", businessData);
            
        } catch (Exception e) {
            // 记录错误日志
            long executionTime = System.currentTimeMillis() - startTime;
            errorMessage = e.getMessage();
            businessData.put("execution_time", executionTime);
            businessData.put("result", "FAILED");
            businessData.put("error", errorMessage);
            
            logUtil.business(methodName, "业务操作失败", businessData);
            logUtil.error(module, methodName, "BUSINESS_ERROR_001", errorMessage, getStackTrace(e));
            
            // 重新抛出异常
            throw e;
        }
        
        return result;
    }
    
    /**
     * 环绕通知：记录安全相关操作日志
     */
    @Around("@annotation(cn.lazylhxzzy.resume_commit.annotation.SecurityLog)")
    public Object logSecurityOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String module = className.replace("ServiceImpl", "");
        
        // 获取请求信息
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";
        
        Object result = null;
        String riskLevel = "LOW";
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 记录成功日志
            logUtil.security("SUCCESS", methodName + "操作成功", riskLevel);
            
        } catch (Exception e) {
            // 记录失败日志
            riskLevel = "HIGH";
            logUtil.security("FAILURE", methodName + "操作失败: " + e.getMessage(), riskLevel);
            
            // 重新抛出异常
            throw e;
        }
        
        return result;
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
     * 获取客户端IP地址
     */
    private String getClientIpAddress(HttpServletRequest request) {
        if (request == null) {
            return "unknown";
        }
        
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
    
    /**
     * 序列化方法参数
     */
    private String serializeParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            // 过滤敏感信息
            Object[] filteredArgs = new Object[args.length];
            for (int i = 0; i < args.length; i++) {
                if (args[i] != null) {
                    String argStr = args[i].toString();
                    // 过滤密码等敏感信息
                    if (argStr.toLowerCase().contains("password") || 
                        argStr.toLowerCase().contains("secret") ||
                        argStr.toLowerCase().contains("token")) {
                        filteredArgs[i] = "***";
                    } else {
                        filteredArgs[i] = args[i];
                    }
                } else {
                    filteredArgs[i] = null;
                }
            }
            return objectMapper.writeValueAsString(filteredArgs);
        } catch (Exception e) {
            return "参数序列化失败: " + e.getMessage();
        }
    }
    
    /**
     * 获取异常堆栈信息
     */
    private String getStackTrace(Exception e) {
        if (e == null) {
            return null;
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append(e.getClass().getSimpleName()).append(": ").append(e.getMessage()).append("\n");
        
        StackTraceElement[] stackTrace = e.getStackTrace();
        for (int i = 0; i < Math.min(stackTrace.length, 10); i++) {
            sb.append("    at ").append(stackTrace[i].toString()).append("\n");
        }
        
        return sb.toString();
    }
    
    /**
     * 获取当前用户ID
     */
    private Long getCurrentUserId() {
        try {
            String username = getCurrentUsername();
            if (username != null) {
                // 这里需要注入UserMapper，暂时返回null
                return null;
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
                    // 这里需要注入JwtUtil，暂时返回null
                    return null;
                }
            }
        } catch (Exception e) {
            // 忽略异常，返回null
        }
        return null;
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
}
