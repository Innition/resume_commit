-- 日志告警系统数据库表结构

-- 告警配置表
CREATE TABLE log_alert_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_name VARCHAR(100) NOT NULL COMMENT '告警名称',
    alert_type VARCHAR(50) NOT NULL COMMENT '告警类型：ERROR_COUNT, PERFORMANCE, SECURITY',
    log_level VARCHAR(10) COMMENT '日志级别',
    module VARCHAR(100) COMMENT '模块名称',
    condition_text VARCHAR(500) COMMENT '告警条件描述',
    threshold INT NOT NULL COMMENT '阈值',
    notification_type VARCHAR(20) NOT NULL COMMENT '通知类型：EMAIL, WEBHOOK, SMS',
    notification_target VARCHAR(500) COMMENT '通知目标',
    enabled BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    description TEXT COMMENT '描述',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- 告警记录表
CREATE TABLE log_alerts (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    alert_name VARCHAR(100) NOT NULL COMMENT '告警名称',
    alert_type VARCHAR(50) NOT NULL COMMENT '告警类型',
    severity VARCHAR(20) NOT NULL COMMENT '严重程度：LOW, MEDIUM, HIGH, CRITICAL',
    message TEXT NOT NULL COMMENT '告警消息',
    details TEXT COMMENT '详细信息',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING, SENT, FAILED',
    notification_type VARCHAR(20) COMMENT '通知类型',
    notification_target VARCHAR(500) COMMENT '通知目标',
    triggered_at DATETIME NOT NULL COMMENT '触发时间',
    sent_at DATETIME COMMENT '发送时间',
    error_message TEXT COMMENT '错误信息',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建索引
CREATE INDEX idx_alert_configs_type ON log_alert_configs(alert_type);
CREATE INDEX idx_alert_configs_enabled ON log_alert_configs(enabled);
CREATE INDEX idx_alerts_status ON log_alerts(status);
CREATE INDEX idx_alerts_severity ON log_alerts(severity);
CREATE INDEX idx_alerts_triggered_at ON log_alerts(triggered_at);

-- 插入默认告警配置
INSERT INTO log_alert_configs (alert_name, alert_type, log_level, module, condition_text, threshold, notification_type, notification_target, enabled, description) VALUES
('错误日志告警', 'ERROR_COUNT', 'ERROR', NULL, '5分钟内错误日志数量超过阈值', 10, 'EMAIL', 'admin@example.com', TRUE, '监控系统错误日志，当5分钟内错误数量超过10条时触发告警'),
('性能告警', 'PERFORMANCE', NULL, NULL, '响应时间超过阈值', 5000, 'WEBHOOK', 'http://monitoring.example.com/webhook', TRUE, '监控API响应时间，当响应时间超过5秒时触发告警'),
('安全告警', 'SECURITY', 'WARN', NULL, '1小时内安全事件数量超过阈值', 5, 'SMS', '+8613800138000', TRUE, '监控安全相关日志，当1小时内安全事件超过5次时触发告警');
