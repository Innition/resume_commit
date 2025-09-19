package cn.lazylhxzzy.resume_commit.controller;

import cn.lazylhxzzy.resume_commit.entity.AccessLog;
import cn.lazylhxzzy.resume_commit.entity.SecurityLog;
import cn.lazylhxzzy.resume_commit.entity.SystemLog;
import cn.lazylhxzzy.resume_commit.mapper.AccessLogMapper;
import cn.lazylhxzzy.resume_commit.mapper.SecurityLogMapper;
import cn.lazylhxzzy.resume_commit.mapper.SystemLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志管理控制器
 */
@Tag(name = "日志管理", description = "日志查询和管理接口")
@RestController
@RequestMapping("/logs")
public class LogController {
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    @Autowired
    private AccessLogMapper accessLogMapper;
    
    @Autowired
    private SecurityLogMapper securityLogMapper;
    
    @Operation(summary = "查询系统日志", description = "分页查询系统日志")
    @GetMapping("/system")
    public Map<String, Object> getSystemLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "日志级别") @RequestParam(required = false) String level,
            @Parameter(description = "模块名称") @RequestParam(required = false) String module,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        QueryWrapper<SystemLog> queryWrapper = new QueryWrapper<>();
        
        if (level != null && !level.trim().isEmpty()) {
            queryWrapper.eq("log_level", level);
        }
        if (module != null && !module.trim().isEmpty()) {
            queryWrapper.like("module", module);
        }
        if (startTime != null) {
            queryWrapper.ge("created_at", startTime);
        }
        if (endTime != null) {
            queryWrapper.le("created_at", endTime);
        }
        
        queryWrapper.orderByDesc("created_at");
        
        Page<SystemLog> pageResult = new Page<>(page, size);
        Page<SystemLog> result = systemLogMapper.selectPage(pageResult, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    @Operation(summary = "查询访问日志", description = "分页查询访问日志")
    @GetMapping("/access")
    public Map<String, Object> getAccessLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "IP地址") @RequestParam(required = false) String ipAddress,
            @Parameter(description = "请求方法") @RequestParam(required = false) String requestMethod,
            @Parameter(description = "响应状态码") @RequestParam(required = false) Integer responseCode,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        QueryWrapper<AccessLog> queryWrapper = new QueryWrapper<>();
        
        if (username != null && !username.trim().isEmpty()) {
            queryWrapper.like("username", username);
        }
        if (ipAddress != null && !ipAddress.trim().isEmpty()) {
            queryWrapper.like("ip_address", ipAddress);
        }
        if (requestMethod != null && !requestMethod.trim().isEmpty()) {
            queryWrapper.eq("request_method", requestMethod);
        }
        if (responseCode != null) {
            queryWrapper.eq("response_code", responseCode);
        }
        if (startTime != null) {
            queryWrapper.ge("created_at", startTime);
        }
        if (endTime != null) {
            queryWrapper.le("created_at", endTime);
        }
        
        queryWrapper.orderByDesc("created_at");
        
        Page<AccessLog> pageResult = new Page<>(page, size);
        Page<AccessLog> result = accessLogMapper.selectPage(pageResult, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    @Operation(summary = "查询安全日志", description = "分页查询安全日志")
    @GetMapping("/security")
    public Map<String, Object> getSecurityLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "日志类型") @RequestParam(required = false) String logType,
            @Parameter(description = "风险级别") @RequestParam(required = false) String riskLevel,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        QueryWrapper<SecurityLog> queryWrapper = new QueryWrapper<>();
        
        if (logType != null && !logType.trim().isEmpty()) {
            queryWrapper.eq("log_type", logType);
        }
        if (riskLevel != null && !riskLevel.trim().isEmpty()) {
            queryWrapper.eq("risk_level", riskLevel);
        }
        if (username != null && !username.trim().isEmpty()) {
            queryWrapper.like("username", username);
        }
        if (startTime != null) {
            queryWrapper.ge("created_at", startTime);
        }
        if (endTime != null) {
            queryWrapper.le("created_at", endTime);
        }
        
        queryWrapper.orderByDesc("created_at");
        
        Page<SecurityLog> pageResult = new Page<>(page, size);
        Page<SecurityLog> result = securityLogMapper.selectPage(pageResult, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    @Operation(summary = "查询错误日志", description = "分页查询错误日志")
    @GetMapping("/error")
    public Map<String, Object> getErrorLogs(
            @Parameter(description = "页码") @RequestParam(defaultValue = "1") Integer page,
            @Parameter(description = "每页大小") @RequestParam(defaultValue = "20") Integer size,
            @Parameter(description = "模块名称") @RequestParam(required = false) String module,
            @Parameter(description = "操作名称") @RequestParam(required = false) String operation,
            @Parameter(description = "用户名") @RequestParam(required = false) String username,
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        QueryWrapper<SystemLog> queryWrapper = new QueryWrapper<>();
        
        // 只查询ERROR级别的日志
        queryWrapper.eq("log_level", "ERROR");
        
        if (module != null && !module.trim().isEmpty()) {
            queryWrapper.like("logger_name", module);
        }
        if (operation != null && !operation.trim().isEmpty()) {
            queryWrapper.like("message", operation);
        }
        if (username != null && !username.trim().isEmpty()) {
            queryWrapper.like("message", username);
        }
        if (startTime != null) {
            queryWrapper.ge("created_at", startTime);
        }
        if (endTime != null) {
            queryWrapper.le("created_at", endTime);
        }
        
        queryWrapper.orderByDesc("created_at");
        
        Page<SystemLog> pageResult = new Page<>(page, size);
        Page<SystemLog> result = systemLogMapper.selectPage(pageResult, queryWrapper);
        
        Map<String, Object> response = new HashMap<>();
        response.put("records", result.getRecords());
        response.put("total", result.getTotal());
        response.put("pages", result.getPages());
        response.put("current", result.getCurrent());
        response.put("size", result.getSize());
        
        return response;
    }
    
    @Operation(summary = "获取日志统计", description = "获取各类日志的统计信息")
    @GetMapping("/statistics")
    public Map<String, Object> getLogStatistics(
            @Parameter(description = "开始时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startTime,
            @Parameter(description = "结束时间") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endTime) {
        
        Map<String, Object> statistics = new HashMap<>();
        
        // 系统日志统计
        QueryWrapper<SystemLog> systemQuery = new QueryWrapper<>();
        if (startTime != null) {
            systemQuery.ge("created_at", startTime);
        }
        if (endTime != null) {
            systemQuery.le("created_at", endTime);
        }
        
        Long systemLogCount = systemLogMapper.selectCount(systemQuery);
        statistics.put("systemLogCount", systemLogCount);
        
        // 访问日志统计
        QueryWrapper<AccessLog> accessQuery = new QueryWrapper<>();
        if (startTime != null) {
            accessQuery.ge("created_at", startTime);
        }
        if (endTime != null) {
            accessQuery.le("created_at", endTime);
        }
        
        Long accessLogCount = accessLogMapper.selectCount(accessQuery);
        statistics.put("accessLogCount", accessLogCount);
        
        // 安全日志统计
        QueryWrapper<SecurityLog> securityQuery = new QueryWrapper<>();
        if (startTime != null) {
            securityQuery.ge("created_at", startTime);
        }
        if (endTime != null) {
            securityQuery.le("created_at", endTime);
        }
        
        Long securityLogCount = securityLogMapper.selectCount(securityQuery);
        statistics.put("securityLogCount", securityLogCount);
        
        // 错误日志统计
        QueryWrapper<SystemLog> errorQuery = new QueryWrapper<>();
        errorQuery.eq("log_level", "ERROR");
        if (startTime != null) {
            errorQuery.ge("created_at", startTime);
        }
        if (endTime != null) {
            errorQuery.le("created_at", endTime);
        }
        
        Long errorLogCount = systemLogMapper.selectCount(errorQuery);
        statistics.put("errorLogCount", errorLogCount);
        
        return statistics;
    }
    
    @Operation(summary = "清理过期日志", description = "清理指定天数之前的日志")
    @DeleteMapping("/cleanup")
    public Map<String, Object> cleanupLogs(
            @Parameter(description = "保留天数") @RequestParam(defaultValue = "30") Integer days) {
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 清理系统日志
            QueryWrapper<SystemLog> systemQuery = new QueryWrapper<>();
            systemQuery.lt("created_at", cutoffTime);
            int systemDeleted = systemLogMapper.delete(systemQuery);
            
            // 清理访问日志
            QueryWrapper<AccessLog> accessQuery = new QueryWrapper<>();
            accessQuery.lt("created_at", cutoffTime);
            int accessDeleted = accessLogMapper.delete(accessQuery);
            
            // 清理安全日志
            QueryWrapper<SecurityLog> securityQuery = new QueryWrapper<>();
            securityQuery.lt("created_at", cutoffTime);
            int securityDeleted = securityLogMapper.delete(securityQuery);
            
            result.put("success", true);
            result.put("message", "日志清理完成");
            result.put("systemLogsDeleted", systemDeleted);
            result.put("accessLogsDeleted", accessDeleted);
            result.put("securityLogsDeleted", securityDeleted);
            result.put("totalDeleted", systemDeleted + accessDeleted + securityDeleted);
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "日志清理失败: " + e.getMessage());
        }
        
        return result;
    }
}
