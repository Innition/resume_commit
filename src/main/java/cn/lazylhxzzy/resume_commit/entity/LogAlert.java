package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 日志告警记录实体
 */
@Data
@TableName("log_alerts")
public class LogAlert {
    @TableId(type = IdType.AUTO)
    private Long id;
    
    private String alertName;
    private String alertType;
    private String severity; // LOW, MEDIUM, HIGH, CRITICAL
    private String message;
    private String details;
    private String status; // PENDING, SENT, FAILED
    private String notificationType;
    private String notificationTarget;
    private LocalDateTime triggeredAt;
    private LocalDateTime sentAt;
    private String errorMessage;
    private LocalDateTime createdAt;
}
