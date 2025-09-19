package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志告警配置实体
 */
@Data
@TableName("log_alert_configs")
public class LogAlertConfig {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String alertName;
    private String alertType; // ERROR_COUNT, PERFORMANCE, SECURITY
    private String logLevel;
    private String module;
    private String conditionText; // 告警条件，如：count > 10
    private Integer threshold; // 阈值
    private String notificationType; // EMAIL, WEBHOOK, SMS
    private String notificationTarget; // 通知目标
    private Boolean enabled; // 是否启用
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
