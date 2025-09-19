package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 安全日志实体
 */
@Data
@TableName("security_logs")
public class SecurityLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 安全事件类型
     */
    private String logType;
    
    /**
     * 用户ID
     */
    private Long userId;
    
    /**
     * 用户名
     */
    private String username;
    
    /**
     * IP地址
     */
    private String ipAddress;
    
    /**
     * 用户代理
     */
    private String userAgent;
    
    /**
     * 事件描述
     */
    private String eventDescription;
    
    /**
     * 风险级别
     */
    private String riskLevel;
    
    /**
     * 额外数据(JSON格式)
     */
    private String additionalData;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
