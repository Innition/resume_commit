package cn.lazylhxzzy.resume_commit.aspect;

import cn.lazylhxzzy.resume_commit.util.LogUtil;
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
 * 安全审计切面
 */
@Aspect
@Component
public class SecurityAuditAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(SecurityAuditAspect.class);
    
    @Autowired
    private LogUtil logUtil;
    
    /**
     * 监控认证相关操作
     */
    @Around("execution(* cn.lazylhxzzy.resume_commit.service.impl.UserServiceImpl.login(..)) || " +
            "execution(* cn.lazylhxzzy.resume_commit.service.impl.UserServiceImpl.register(..))")
    public Object auditAuthentication(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditSecurityOperation(joinPoint, "AUTHENTICATION");
    }
    
    /**
     * 监控权限相关操作
     */
    @Around("execution(* cn.lazylhxzzy.resume_commit.service.impl.UserServiceImpl.generateInviteCode(..)) || " +
            "execution(* cn.lazylhxzzy.resume_commit.service.impl.UserServiceImpl.promoteUser(..))")
    public Object auditAuthorization(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditSecurityOperation(joinPoint, "AUTHORIZATION");
    }
    
    /**
     * 监控数据操作
     */
    @Around("execution(* cn.lazylhxzzy.resume_commit.service.impl.ResumeRecordServiceImpl.addRecord(..)) || " +
            "execution(* cn.lazylhxzzy.resume_commit.service.impl.ResumeRecordServiceImpl.updateRecord(..)) || " +
            "execution(* cn.lazylhxzzy.resume_commit.service.impl.ResumeRecordServiceImpl.deleteRecord(..))")
    public Object auditDataOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditSecurityOperation(joinPoint, "DATA_OPERATION");
    }
    
    /**
     * 监控文件操作
     */
    @Around("execution(* cn.lazylhxzzy.resume_commit.service.impl.ResumeRecordServiceImpl.exportUserRecords(..)) || " +
            "execution(* cn.lazylhxzzy.resume_commit.service.impl.ResumeRecordServiceImpl.exportAllRecords(..)) || " +
            "execution(* cn.lazylhxzzy.resume_commit.service.impl.ResumeRecordServiceImpl.importUserRecords(..)) || " +
            "execution(* cn.lazylhxzzy.resume_commit.service.impl.ResumeRecordServiceImpl.importAllRecords(..))")
    public Object auditFileOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        return auditSecurityOperation(joinPoint, "FILE_OPERATION");
    }
    
    /**
     * 通用安全审计方法
     */
    private Object auditSecurityOperation(ProceedingJoinPoint joinPoint, String operationType) throws Throwable {
        long startTime = System.currentTimeMillis();
        
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        String methodName = method.getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String module = className.replace("ServiceImpl", "");
        
        // 获取请求信息
        HttpServletRequest request = getCurrentRequest();
        String ipAddress = getClientIpAddress(request);
        String userAgent = request != null ? request.getHeader("User-Agent") : "UNKNOWN";
        
        // 获取方法参数（过滤敏感信息）
        Object[] args = joinPoint.getArgs();
        String parameters = sanitizeParameters(args);
        
        Object result = null;
        Exception exception = null;
        String riskLevel = "LOW";
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            
            // 根据操作类型和结果确定风险级别
            riskLevel = determineRiskLevel(operationType, methodName, result, null);
            
            // 记录成功的安全审计日志
            logSecurityAudit(module, methodName, operationType, "SUCCESS", riskLevel, 
                           ipAddress, userAgent, parameters, result, null);
            
            return result;
            
        } catch (Exception e) {
            exception = e;
            
            // 根据异常类型确定风险级别
            riskLevel = determineRiskLevel(operationType, methodName, null, e);
            
            // 记录失败的安全审计日志
            logSecurityAudit(module, methodName, operationType, "FAILURE", riskLevel, 
                           ipAddress, userAgent, parameters, null, e);
            
            throw e;
        }
    }
    
    /**
     * 记录安全审计日志
     */
    private void logSecurityAudit(String module, String methodName, String operationType, 
                                String result, String riskLevel, String ipAddress, 
                                String userAgent, String parameters, Object returnValue, 
                                Exception exception) {
        try {
            // 构建审计数据
            Map<String, Object> auditData = new HashMap<>();
            auditData.put("operation_type", operationType);
            auditData.put("method_name", methodName);
            auditData.put("result", result);
            auditData.put("risk_level", riskLevel);
            auditData.put("parameters", parameters);
            auditData.put("has_exception", exception != null);
            
            if (exception != null) {
                auditData.put("exception_type", exception.getClass().getSimpleName());
                auditData.put("exception_message", exception.getMessage());
            }
            
            if (returnValue != null) {
                auditData.put("return_type", returnValue.getClass().getSimpleName());
            }
            
            // 记录到安全日志
            logUtil.security(operationType, 
                           String.format("%s.%s - %s", module, methodName, result), 
                           riskLevel);
            
            // 记录到业务日志
            logUtil.business(methodName, 
                           String.format("安全审计 - %s操作%s", operationType, result), 
                           auditData);
            
            // 高风险操作记录到系统日志
            if ("HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel)) {
                logUtil.warn(module, methodName, 
                           String.format("高风险操作: %s.%s - %s - IP: %s", 
                                       module, methodName, result, ipAddress));
            }
            
        } catch (Exception e) {
            logger.error("记录安全审计日志失败", e);
        }
    }
    
    /**
     * 确定风险级别
     */
    private String determineRiskLevel(String operationType, String methodName, 
                                    Object result, Exception exception) {
        // 根据操作类型确定基础风险级别
        switch (operationType) {
            case "AUTHENTICATION":
                if (exception != null) return "HIGH"; // 认证失败
                return "MEDIUM";
            case "AUTHORIZATION":
                if (exception != null) return "CRITICAL"; // 权限操作失败
                return "HIGH";
            case "DATA_OPERATION":
                if ("deleteRecord".equals(methodName)) return "HIGH"; // 删除操作
                if ("updateRecord".equals(methodName)) return "MEDIUM"; // 更新操作
                return "LOW";
            case "FILE_OPERATION":
                if (methodName.contains("export") || methodName.contains("import")) {
                    return "HIGH"; // 文件导出导入
                }
                return "MEDIUM";
            default:
                return "LOW";
        }
    }
    
    /**
     * 清理敏感参数
     */
    private String sanitizeParameters(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }
        
        try {
            StringBuilder sb = new StringBuilder("[");
            for (int i = 0; i < args.length; i++) {
                if (i > 0) sb.append(", ");
                
                if (args[i] == null) {
                    sb.append("null");
                } else {
                    String argStr = args[i].toString();
                    // 过滤敏感信息
                    if (argStr.toLowerCase().contains("password") || 
                        argStr.toLowerCase().contains("secret") ||
                        argStr.toLowerCase().contains("token") ||
                        argStr.toLowerCase().contains("key")) {
                        sb.append("***");
                    } else {
                        sb.append(argStr);
                    }
                }
            }
            sb.append("]");
            return sb.toString();
        } catch (Exception e) {
            return "参数解析失败";
        }
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
}
