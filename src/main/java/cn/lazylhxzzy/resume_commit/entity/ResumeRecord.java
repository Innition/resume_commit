package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 投递记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("resume_records")
public class ResumeRecord {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long userId;
    
    private String companyName;
    
    private String position;
    
    private String baseLocation;
    
    private LocalDateTime applyTime;
    
    private LocalDateTime testTime;
    
    private LocalDateTime writtenExamTime;
    
    private String currentStatus; // 已投递, 已测评, 已笔试, 已面试
    
    private LocalDateTime currentStatusDate;
    
    private String finalResult; // 简历挂, 测评挂, 笔试挂, 面试挂, OC, PENDING
    
    private String expectedSalaryType; // 总包, 月薪, 待商议
    
    private String expectedSalaryValue; // 薪资数值
    
    private String remarks;
    
    private String companyUrl;
    
    private String companyGroupId;
    
    private Boolean isPrimary;
    
    private LocalDateTime createdAt;
    
    private LocalDateTime updatedAt;
}
