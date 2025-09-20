package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 性能监控日志实体
 */
@Data
@TableName("performance_logs")
public class PerformanceLog {
    
    @TableId(type = IdType.AUTO)
    private Long id;
    
    /**
     * 模块名称
     */
    private String module;
    
    /**
     * 方法名称
     */
    private String methodName;
    
    /**
     * 执行时间(毫秒)
     */
    private Long executionTime;
    
    /**
     * 内存使用量(字节)
     */
    private Long memoryUsage;
    
    /**
     * CPU使用率(%)
     */
    private BigDecimal cpuUsage;
    
    /**
     * 线程数
     */
    private Integer threadCount;
    
    /**
     * GC次数
     */
    private Long gcCount;
    
    /**
     * GC时间(毫秒)
     */
    private Long gcTime;
    
    /**
     * 额外指标(JSON格式)
     */
    private String additionalMetrics;
    
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
