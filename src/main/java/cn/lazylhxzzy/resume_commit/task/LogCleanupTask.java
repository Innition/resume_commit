package cn.lazylhxzzy.resume_commit.task;

import cn.lazylhxzzy.resume_commit.entity.AccessLog;
import cn.lazylhxzzy.resume_commit.entity.LogAlert;
import cn.lazylhxzzy.resume_commit.entity.SecurityLog;
import cn.lazylhxzzy.resume_commit.entity.SystemLog;
import cn.lazylhxzzy.resume_commit.mapper.AccessLogMapper;
import cn.lazylhxzzy.resume_commit.mapper.LogAlertMapper;
import cn.lazylhxzzy.resume_commit.mapper.SecurityLogMapper;
import cn.lazylhxzzy.resume_commit.mapper.SystemLogMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * 日志清理定时任务
 */
@Component
public class LogCleanupTask {
    
    private static final Logger logger = LoggerFactory.getLogger(LogCleanupTask.class);
    
    @Autowired
    private SystemLogMapper systemLogMapper;
    
    @Autowired
    private AccessLogMapper accessLogMapper;
    
    @Autowired
    private SecurityLogMapper securityLogMapper;
    
    @Autowired
    private LogAlertMapper alertMapper;
    
    @Value("${log.cleanup.system.days:30}")
    private int systemLogRetentionDays;
    
    @Value("${log.cleanup.access.days:7}")
    private int accessLogRetentionDays;
    
    @Value("${log.cleanup.security.days:90}")
    private int securityLogRetentionDays;
    
    @Value("${log.cleanup.alert.days:60}")
    private int alertRetentionDays;
    
    @Value("${log.cleanup.enabled:true}")
    private boolean cleanupEnabled;
    
    /**
     * 每天凌晨2点执行日志清理任务
     */
    @Scheduled(cron = "0 0 2 * * ?")
    public void cleanupLogs() {
        if (!cleanupEnabled) {
            logger.info("日志清理功能已禁用");
            return;
        }
        
        logger.info("开始执行日志清理任务");
        
        try {
            // 清理系统日志
            cleanupSystemLogs();
            
            // 清理访问日志
            cleanupAccessLogs();
            
            // 清理安全日志
            cleanupSecurityLogs();
            
            // 清理告警记录
            cleanupAlertLogs();
            
            logger.info("日志清理任务执行完成");
            
        } catch (Exception e) {
            logger.error("日志清理任务执行失败", e);
        }
    }
    
    /**
     * 清理系统日志
     */
    private void cleanupSystemLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(systemLogRetentionDays);
            
            QueryWrapper<SystemLog> queryWrapper = new QueryWrapper<>();
            queryWrapper.lt("created_at", cutoffTime);
            
            int deletedCount = systemLogMapper.delete(queryWrapper);
            
            if (deletedCount > 0) {
                logger.info("清理系统日志完成，删除 {} 条记录", deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("清理系统日志失败", e);
        }
    }
    
    /**
     * 清理访问日志
     */
    private void cleanupAccessLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(accessLogRetentionDays);
            
            QueryWrapper<AccessLog> queryWrapper = new QueryWrapper<>();
            queryWrapper.lt("created_at", cutoffTime);
            
            int deletedCount = accessLogMapper.delete(queryWrapper);
            
            if (deletedCount > 0) {
                logger.info("清理访问日志完成，删除 {} 条记录", deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("清理访问日志失败", e);
        }
    }
    
    /**
     * 清理安全日志
     */
    private void cleanupSecurityLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(securityLogRetentionDays);
            
            QueryWrapper<SecurityLog> queryWrapper = new QueryWrapper<>();
            queryWrapper.lt("created_at", cutoffTime);
            
            int deletedCount = securityLogMapper.delete(queryWrapper);
            
            if (deletedCount > 0) {
                logger.info("清理安全日志完成，删除 {} 条记录", deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("清理安全日志失败", e);
        }
    }
    
    /**
     * 清理告警记录
     */
    private void cleanupAlertLogs() {
        try {
            LocalDateTime cutoffTime = LocalDateTime.now().minusDays(alertRetentionDays);
            
            QueryWrapper<LogAlert> queryWrapper = new QueryWrapper<>();
            queryWrapper.lt("created_at", cutoffTime);
            
            int deletedCount = alertMapper.delete(queryWrapper);
            
            if (deletedCount > 0) {
                logger.info("清理告警记录完成，删除 {} 条记录", deletedCount);
            }
            
        } catch (Exception e) {
            logger.error("清理告警记录失败", e);
        }
    }
    
    /**
     * 手动清理指定天数的日志
     */
    public void manualCleanup(int days) {
        logger.info("开始手动清理 {} 天前的日志", days);
        
        LocalDateTime cutoffTime = LocalDateTime.now().minusDays(days);
        int totalDeleted = 0;
        
        try {
            // 清理系统日志
            QueryWrapper<SystemLog> systemQuery = new QueryWrapper<>();
            systemQuery.lt("created_at", cutoffTime);
            int systemDeleted = systemLogMapper.delete(systemQuery);
            totalDeleted += systemDeleted;
            
            // 清理访问日志
            QueryWrapper<AccessLog> accessQuery = new QueryWrapper<>();
            accessQuery.lt("created_at", cutoffTime);
            int accessDeleted = accessLogMapper.delete(accessQuery);
            totalDeleted += accessDeleted;
            
            // 清理安全日志
            QueryWrapper<SecurityLog> securityQuery = new QueryWrapper<>();
            securityQuery.lt("created_at", cutoffTime);
            int securityDeleted = securityLogMapper.delete(securityQuery);
            totalDeleted += securityDeleted;
            
            // 清理告警记录
            QueryWrapper<LogAlert> alertQuery = new QueryWrapper<>();
            alertQuery.lt("created_at", cutoffTime);
            int alertDeleted = alertMapper.delete(alertQuery);
            totalDeleted += alertDeleted;
            
            logger.info("手动清理完成，总共删除 {} 条记录", totalDeleted);
            
        } catch (Exception e) {
            logger.error("手动清理失败", e);
            throw e;
        }
    }
}
