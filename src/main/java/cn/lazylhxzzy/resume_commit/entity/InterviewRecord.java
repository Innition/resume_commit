package cn.lazylhxzzy.resume_commit.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 面试记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = false)
@TableName("interview_records")
public class InterviewRecord {
    
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    
    private Long resumeRecordId;
    
    private String interviewType; // AI面, 一面, 二面, 三面, 四面, 五面, 六面, 七面, 八面, 九面, 十面
    
    private LocalDateTime interviewTime;
    
    private LocalDateTime createdAt;
}
