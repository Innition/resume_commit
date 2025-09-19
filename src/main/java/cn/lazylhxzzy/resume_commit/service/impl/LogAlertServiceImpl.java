package cn.lazylhxzzy.resume_commit.service.impl;

import cn.lazylhxzzy.resume_commit.entity.LogAlert;
import cn.lazylhxzzy.resume_commit.entity.LogAlertConfig;
import cn.lazylhxzzy.resume_commit.entity.SystemLog;
import cn.lazylhxzzy.resume_commit.mapper.LogAlertConfigMapper;
import cn.lazylhxzzy.resume_commit.mapper.LogAlertMapper;
import cn.lazylhxzzy.resume_commit.mapper.SystemLogMapper;
import cn.lazylhxzzy.resume_commit.service.LogAlertService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 日志告警服务实现类
 */
@Service
public class LogAlertServiceImpl implements LogAlertService {
    
    private static final Logger logger = LoggerFactory.getLogger(LogAlertServiceImpl.class);
    
    @Autowired
    private LogAlertConfigMapper alertConfigMapper;
    
    @Autowired
    private LogAlertMapper alertMapper;
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    @Override
    @Scheduled(fixedRate = 60000) // 每分钟检查一次
    public void checkAndTriggerAlerts() {
        try {
            List<LogAlertConfig> configs = alertConfigMapper.selectList(
                new QueryWrapper<LogAlertConfig>().eq("enabled", true)
            );
            
            for (LogAlertConfig config : configs) {
                checkAlert(config);
            }
        } catch (Exception e) {
            logger.error("检查告警失败", e);
        }
    }
    
    /**
     * 检查单个告警配置
     */
    private void checkAlert(LogAlertConfig config) {
        try {
            boolean shouldTrigger = false;
            String message = "";
            String severity = "MEDIUM";
            
            switch (config.getAlertType()) {
                case "ERROR_COUNT":
                    shouldTrigger = checkErrorCountAlert(config);
                    message = "错误日志数量超过阈值";
                    severity = "HIGH";
                    break;
                case "PERFORMANCE":
                    shouldTrigger = checkPerformanceAlert(config);
                    message = "性能指标异常";
                    severity = "MEDIUM";
                    break;
                case "SECURITY":
                    shouldTrigger = checkSecurityAlert(config);
                    message = "安全事件告警";
                    severity = "CRITICAL";
                    break;
            }
            
            if (shouldTrigger) {
                triggerAlert(config, message, severity);
            }
        } catch (Exception e) {
            logger.error("检查告警配置失败: " + config.getAlertName(), e);
        }
    }
    
    /**
     * 检查错误数量告警
     */
    private boolean checkErrorCountAlert(LogAlertConfig config) {
        QueryWrapper<SystemLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("log_level", "ERROR");
        
        if (config.getModule() != null && !config.getModule().isEmpty()) {
            queryWrapper.eq("module", config.getModule());
        }
        
        // 检查最近5分钟的错误数量
        LocalDateTime fiveMinutesAgo = LocalDateTime.now().minusMinutes(5);
        queryWrapper.ge("created_at", fiveMinutesAgo);
        
        Long errorCount = systemLogMapper.selectCount(queryWrapper);
        return errorCount >= config.getThreshold();
    }
    
    /**
     * 检查性能告警
     */
    private boolean checkPerformanceAlert(LogAlertConfig config) {
        QueryWrapper<SystemLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("log_type", "PERFORMANCE");
        queryWrapper.gt("response_time", config.getThreshold());
        
        // 检查最近10分钟的性能数据
        LocalDateTime tenMinutesAgo = LocalDateTime.now().minusMinutes(10);
        queryWrapper.ge("created_at", tenMinutesAgo);
        
        Long performanceCount = systemLogMapper.selectCount(queryWrapper);
        return performanceCount >= 3; // 10分钟内超过3次性能异常
    }
    
    /**
     * 检查安全告警
     */
    private boolean checkSecurityAlert(LogAlertConfig config) {
        QueryWrapper<SystemLog> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("log_type", "SECURITY");
        queryWrapper.eq("log_level", "WARN");
        
        // 检查最近1小时的安全事件
        LocalDateTime oneHourAgo = LocalDateTime.now().minusHours(1);
        queryWrapper.ge("created_at", oneHourAgo);
        
        Long securityCount = systemLogMapper.selectCount(queryWrapper);
        return securityCount >= config.getThreshold();
    }
    
    /**
     * 触发告警
     */
    private void triggerAlert(LogAlertConfig config, String message, String severity) {
        try {
            // 检查是否已经存在相同的告警（避免重复告警）
            QueryWrapper<LogAlert> existingQuery = new QueryWrapper<>();
            existingQuery.eq("alert_name", config.getAlertName())
                        .eq("status", "PENDING")
                        .ge("triggered_at", LocalDateTime.now().minusMinutes(30));
            
            LogAlert existingAlert = alertMapper.selectOne(existingQuery);
            if (existingAlert != null) {
                return; // 30分钟内已有相同告警，不重复触发
            }
            
            // 创建告警记录
            LogAlert alert = new LogAlert();
            alert.setAlertName(config.getAlertName());
            alert.setAlertType(config.getAlertType());
            alert.setSeverity(severity);
            alert.setMessage(message);
            alert.setDetails("阈值: " + config.getThreshold() + ", 模块: " + config.getModule());
            alert.setStatus("PENDING");
            alert.setNotificationType(config.getNotificationType());
            alert.setNotificationTarget(config.getNotificationTarget());
            alert.setTriggeredAt(LocalDateTime.now());
            alert.setCreatedAt(LocalDateTime.now());
            
            alertMapper.insert(alert);
            
            // 发送告警通知
            sendAlert(alert);
            
        } catch (Exception e) {
            logger.error("触发告警失败: " + config.getAlertName(), e);
        }
    }
    
    @Override
    public void sendAlert(LogAlert alert) {
        try {
            // 这里可以实现具体的通知逻辑
            // 例如：发送邮件、调用webhook、发送短信等
            
            switch (alert.getNotificationType()) {
                case "EMAIL":
                    sendEmailAlert(alert);
                    break;
                case "WEBHOOK":
                    sendWebhookAlert(alert);
                    break;
                case "SMS":
                    sendSmsAlert(alert);
                    break;
                default:
                    logger.warn("未知的通知类型: " + alert.getNotificationType());
            }
            
            // 更新告警状态
            alert.setStatus("SENT");
            alert.setSentAt(LocalDateTime.now());
            alertMapper.updateById(alert);
            
        } catch (Exception e) {
            logger.error("发送告警通知失败", e);
            alert.setStatus("FAILED");
            alert.setErrorMessage(e.getMessage());
            alertMapper.updateById(alert);
        }
    }
    
    /**
     * 发送邮件告警
     */
    private void sendEmailAlert(LogAlert alert) {
        // TODO: 实现邮件发送逻辑
        logger.info("发送邮件告警: {} - {}", alert.getAlertName(), alert.getMessage());
    }
    
    /**
     * 发送Webhook告警
     */
    private void sendWebhookAlert(LogAlert alert) {
        // TODO: 实现Webhook发送逻辑
        logger.info("发送Webhook告警: {} - {}", alert.getAlertName(), alert.getMessage());
    }
    
    /**
     * 发送短信告警
     */
    private void sendSmsAlert(LogAlert alert) {
        // TODO: 实现短信发送逻辑
        logger.info("发送短信告警: {} - {}", alert.getAlertName(), alert.getMessage());
    }
    
    @Override
    public List<LogAlertConfig> getAlertConfigs() {
        return alertConfigMapper.selectList(new QueryWrapper<>());
    }
    
    @Override
    public void createAlertConfig(LogAlertConfig config) {
        config.setCreatedAt(LocalDateTime.now());
        config.setUpdatedAt(LocalDateTime.now());
        alertConfigMapper.insert(config);
    }
    
    @Override
    public void updateAlertConfig(LogAlertConfig config) {
        config.setUpdatedAt(LocalDateTime.now());
        alertConfigMapper.updateById(config);
    }
    
    @Override
    public void deleteAlertConfig(Long id) {
        alertConfigMapper.deleteById(id);
    }
    
    @Override
    public List<LogAlert> getAlerts(int page, int size, String status, String severity) {
        QueryWrapper<LogAlert> queryWrapper = new QueryWrapper<>();
        
        if (status != null && !status.isEmpty()) {
            queryWrapper.eq("status", status);
        }
        if (severity != null && !severity.isEmpty()) {
            queryWrapper.eq("severity", severity);
        }
        
        queryWrapper.orderByDesc("triggered_at");
        
        Page<LogAlert> pageResult = new Page<>(page, size);
        Page<LogAlert> result = alertMapper.selectPage(pageResult, queryWrapper);
        
        return result.getRecords();
    }
    
    @Override
    public Map<String, Object> getAlertStatistics() {
        Map<String, Object> statistics = new HashMap<>();
        
        // 总告警数
        Long totalAlerts = alertMapper.selectCount(new QueryWrapper<>());
        statistics.put("totalAlerts", totalAlerts);
        
        // 待处理告警数
        Long pendingAlerts = alertMapper.selectCount(
            new QueryWrapper<LogAlert>().eq("status", "PENDING")
        );
        statistics.put("pendingAlerts", pendingAlerts);
        
        // 已发送告警数
        Long sentAlerts = alertMapper.selectCount(
            new QueryWrapper<LogAlert>().eq("status", "SENT")
        );
        statistics.put("sentAlerts", sentAlerts);
        
        // 失败告警数
        Long failedAlerts = alertMapper.selectCount(
            new QueryWrapper<LogAlert>().eq("status", "FAILED")
        );
        statistics.put("failedAlerts", failedAlerts);
        
        // 按严重程度统计
        Map<String, Long> severityStats = new HashMap<>();
        severityStats.put("CRITICAL", alertMapper.selectCount(
            new QueryWrapper<LogAlert>().eq("severity", "CRITICAL")
        ));
        severityStats.put("HIGH", alertMapper.selectCount(
            new QueryWrapper<LogAlert>().eq("severity", "HIGH")
        ));
        severityStats.put("MEDIUM", alertMapper.selectCount(
            new QueryWrapper<LogAlert>().eq("severity", "MEDIUM")
        ));
        severityStats.put("LOW", alertMapper.selectCount(
            new QueryWrapper<LogAlert>().eq("severity", "LOW")
        ));
        statistics.put("severityStats", severityStats);
        
        return statistics;
    }
}
