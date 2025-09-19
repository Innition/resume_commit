package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 系统日志实体
 */
@Data
@TableName("system_logs")
public class SystemLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 日志级别
     */
    private String logLevel;
    
    /**
     * 日志类型
     */
    private String logType;
    
    /**
     * 模块名称
     */
    private String module;
    
    /**
     * 操作描述
     */
    private String operation;
    
    /**
     * 操作用户ID
     */
    private Long userId;
    
    /**
     * 操作用户名
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
     * 请求方法
     */
    private String requestMethod;
    
    /**
     * 请求URL
     */
    private String requestUrl;
    
    /**
     * 请求参数
     */
    private String requestParams;
    
    /**
     * 响应状态码
     */
    private Integer responseCode;
    
    /**
     * 响应时间(毫秒)
     */
    private Long responseTime;
    
    /**
     * 错误代码
     */
    private String errorCode;
    
    /**
     * 错误信息
     */
    private String errorMessage;
    
    /**
     * 异常堆栈
     */
    private String stackTrace;
    
    /**
     * 额外数据(JSON格式)
     */
    private String extraData;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
