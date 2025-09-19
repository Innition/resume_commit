package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 访问日志实体
 */
@Data
@TableName("access_logs")
public class AccessLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
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
     * 请求大小(字节)
     */
    private Long requestSize;
    
    /**
     * 响应大小(字节)
     */
    private Long responseSize;
    
    /**
     * 来源页面
     */
    private String referer;
    
    /**
     * 会话ID
     */
    private String sessionId;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
