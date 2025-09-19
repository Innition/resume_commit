-- 日志系统数据库表结构

-- 系统日志表
CREATE TABLE system_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_level VARCHAR(10) NOT NULL COMMENT '日志级别：DEBUG, INFO, WARN, ERROR',
    log_type VARCHAR(50) NOT NULL COMMENT '日志类型：API, BUSINESS, SECURITY, SYSTEM, PERFORMANCE',
    module VARCHAR(100) COMMENT '模块名称',
    operation VARCHAR(200) COMMENT '操作描述',
    user_id BIGINT COMMENT '操作用户ID',
    username VARCHAR(50) COMMENT '操作用户名',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    request_method VARCHAR(10) COMMENT '请求方法',
    request_url VARCHAR(500) COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_code INT COMMENT '响应状态码',
    response_time BIGINT COMMENT '响应时间(毫秒)',
    error_code VARCHAR(50) COMMENT '错误代码',
    error_message TEXT COMMENT '错误信息',
    stack_trace TEXT COMMENT '异常堆栈',
    extra_data JSON COMMENT '额外数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_level_type (log_level, log_type),
    INDEX idx_user_id (user_id),
    INDEX idx_created_at (created_at),
    INDEX idx_module (module),
    INDEX idx_operation (operation)
) COMMENT '系统日志表';

-- 访问日志表
CREATE TABLE access_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    request_method VARCHAR(10) NOT NULL COMMENT '请求方法',
    request_url VARCHAR(500) NOT NULL COMMENT '请求URL',
    request_params TEXT COMMENT '请求参数',
    response_code INT NOT NULL COMMENT '响应状态码',
    response_time BIGINT NOT NULL COMMENT '响应时间(毫秒)',
    request_size BIGINT COMMENT '请求大小(字节)',
    response_size BIGINT COMMENT '响应大小(字节)',
    referer VARCHAR(500) COMMENT '来源页面',
    session_id VARCHAR(100) COMMENT '会话ID',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_created_at (created_at),
    INDEX idx_response_code (response_code),
    INDEX idx_request_url (request_url(100))
) COMMENT '访问日志表';

-- 安全日志表
CREATE TABLE security_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    log_type VARCHAR(50) NOT NULL COMMENT '安全事件类型：LOGIN, LOGOUT, PERMISSION_DENIED, SUSPICIOUS_ACTIVITY',
    user_id BIGINT COMMENT '用户ID',
    username VARCHAR(50) COMMENT '用户名',
    ip_address VARCHAR(45) NOT NULL COMMENT 'IP地址',
    user_agent TEXT COMMENT '用户代理',
    event_description TEXT NOT NULL COMMENT '事件描述',
    risk_level VARCHAR(20) DEFAULT 'LOW' COMMENT '风险级别：LOW, MEDIUM, HIGH, CRITICAL',
    additional_data JSON COMMENT '额外数据',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_log_type (log_type),
    INDEX idx_user_id (user_id),
    INDEX idx_ip_address (ip_address),
    INDEX idx_created_at (created_at),
    INDEX idx_risk_level (risk_level)
) COMMENT '安全日志表';

-- 性能监控日志表
CREATE TABLE performance_logs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    module VARCHAR(100) NOT NULL COMMENT '模块名称',
    method_name VARCHAR(200) NOT NULL COMMENT '方法名称',
    execution_time BIGINT NOT NULL COMMENT '执行时间(毫秒)',
    memory_usage BIGINT COMMENT '内存使用量(字节)',
    cpu_usage DECIMAL(5,2) COMMENT 'CPU使用率(%)',
    thread_count INT COMMENT '线程数',
    gc_count BIGINT COMMENT 'GC次数',
    gc_time BIGINT COMMENT 'GC时间(毫秒)',
    additional_metrics JSON COMMENT '额外指标',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_module (module),
    INDEX idx_created_at (created_at),
    INDEX idx_execution_time (execution_time)
) COMMENT '性能监控日志表';

-- 日志配置表
CREATE TABLE log_configs (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT NOT NULL COMMENT '配置值',
    description TEXT COMMENT '配置描述',
    is_active BOOLEAN DEFAULT TRUE COMMENT '是否启用',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT '日志配置表';

-- 插入默认日志配置
INSERT INTO log_configs (config_key, config_value, description) VALUES
('log.retention.days', '30', '日志保留天数'),
('log.cleanup.enabled', 'true', '是否启用日志清理'),
('log.alert.error.threshold', '10', '错误日志告警阈值(每分钟)'),
('log.alert.response.time.threshold', '5000', '响应时间告警阈值(毫秒)'),
('log.async.enabled', 'true', '是否启用异步日志'),
('log.file.max.size', '100MB', '日志文件最大大小'),
('log.file.max.history', '30', '日志文件保留数量');
