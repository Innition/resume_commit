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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * 性能监控切面
 */
@Aspect
@Component
public class PerformanceMonitoringAspect {
    
    private static final Logger logger = LoggerFactory.getLogger(PerformanceMonitoringAspect.class);
    
    @Autowired
    private LogUtil logUtil;
    
    /**
     * 监控所有Controller方法的性能
     */
    @Around("execution(* cn.lazylhxzzy.resume_commit.controller.*.*(..))")
    public Object monitorControllerPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "CONTROLLER");
    }
    
    /**
     * 监控所有Service方法的性能
     */
    @Around("execution(* cn.lazylhxzzy.resume_commit.service.impl.*.*(..))")
    public Object monitorServicePerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "SERVICE");
    }
    
    /**
     * 监控所有Mapper方法的性能
     */
    @Around("execution(* cn.lazylhxzzy.resume_commit.mapper.*.*(..))")
    public Object monitorMapperPerformance(ProceedingJoinPoint joinPoint) throws Throwable {
        return monitorPerformance(joinPoint, "MAPPER");
    }
    
    /**
     * 通用性能监控方法
     */
    private Object monitorPerformance(ProceedingJoinPoint joinPoint, String layer) throws Throwable {
        long startTime = System.currentTimeMillis();
        long startMemory = getUsedMemory();
        
        String methodName = joinPoint.getSignature().getName();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String module = className.replace("Controller", "").replace("ServiceImpl", "").replace("Mapper", "");
        
        Object result = null;
        Exception exception = null;
        
        try {
            // 执行目标方法
            result = joinPoint.proceed();
            return result;
            
        } catch (Exception e) {
            exception = e;
            throw e;
            
        } finally {
            // 计算性能指标
            long endTime = System.currentTimeMillis();
            long endMemory = getUsedMemory();
            
            long executionTime = endTime - startTime;
            long memoryUsed = endMemory - startMemory;
            
            // 记录性能日志
            recordPerformanceLog(module, methodName, executionTime, memoryUsed, layer, exception);
        }
    }
    
    /**
     * 记录性能日志
     */
    private void recordPerformanceLog(String module, String methodName, long executionTime, 
                                    long memoryUsed, String layer, Exception exception) {
        try {
            // 构建性能指标
            Map<String, Object> metrics = new HashMap<>();
            metrics.put("layer", layer);
            metrics.put("execution_time_ms", executionTime);
            metrics.put("memory_used_bytes", memoryUsed);
            metrics.put("memory_used_mb", memoryUsed / 1024.0 / 1024.0);
            metrics.put("thread_name", Thread.currentThread().getName());
            metrics.put("has_exception", exception != null);
            
            if (exception != null) {
                metrics.put("exception_type", exception.getClass().getSimpleName());
                metrics.put("exception_message", exception.getMessage());
            }
            
            // 根据执行时间确定日志级别
            String logLevel = determineLogLevel(executionTime, layer);
            
            // 记录性能日志
            if ("WARN".equals(logLevel) || "ERROR".equals(logLevel)) {
                logUtil.performance(module, methodName, executionTime, metrics);
            }
            
            // 记录到文件日志
            if (executionTime > 1000) { // 超过1秒
                logger.warn("性能监控 - {}:{} 执行时间: {}ms, 内存使用: {}MB", 
                           module, methodName, executionTime, 
                           String.format("%.2f", memoryUsed / 1024.0 / 1024.0));
            } else if (executionTime > 500) { // 超过500ms
                logger.info("性能监控 - {}:{} 执行时间: {}ms, 内存使用: {}MB", 
                           module, methodName, executionTime, 
                           String.format("%.2f", memoryUsed / 1024.0 / 1024.0));
            }
            
        } catch (Exception e) {
            logger.error("记录性能日志失败", e);
        }
    }
    
    /**
     * 确定日志级别
     */
    private String determineLogLevel(long executionTime, String layer) {
        switch (layer) {
            case "CONTROLLER":
                if (executionTime > 5000) return "ERROR"; // 超过5秒
                if (executionTime > 2000) return "WARN";  // 超过2秒
                break;
            case "SERVICE":
                if (executionTime > 3000) return "ERROR"; // 超过3秒
                if (executionTime > 1000) return "WARN";  // 超过1秒
                break;
            case "MAPPER":
                if (executionTime > 2000) return "ERROR"; // 超过2秒
                if (executionTime > 500) return "WARN";   // 超过500ms
                break;
        }
        return "INFO";
    }
    
    /**
     * 获取当前使用的内存
     */
    private long getUsedMemory() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }
}
