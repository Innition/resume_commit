package cn.lazylhxzzy.resume_commit.service;

import cn.lazylhxzzy.resume_commit.entity.LogAlert;
import cn.lazylhxzzy.resume_commit.entity.LogAlertConfig;

import java.util.List;
import java.util.Map;

/**
 * 日志告警服务接口
 */
public interface LogAlertService {
    
    /**
     * 检查并触发告警
     */
    void checkAndTriggerAlerts();
    
    /**
     * 发送告警通知
     */
    void sendAlert(LogAlert alert);
    
    /**
     * 获取告警配置列表
     */
    List<LogAlertConfig> getAlertConfigs();
    
    /**
     * 创建告警配置
     */
    void createAlertConfig(LogAlertConfig config);
    
    /**
     * 更新告警配置
     */
    void updateAlertConfig(LogAlertConfig config);
    
    /**
     * 删除告警配置
     */
    void deleteAlertConfig(Long id);
    
    /**
     * 获取告警记录
     */
    List<LogAlert> getAlerts(int page, int size, String status, String severity);
    
    /**
     * 获取告警统计
     */
    Map<String, Object> getAlertStatistics();
}
