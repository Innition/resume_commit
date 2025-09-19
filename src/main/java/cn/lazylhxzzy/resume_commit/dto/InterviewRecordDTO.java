package cn.lazylhxzzy.resume_commit.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * 面试记录DTO
 */
@Data
public class InterviewRecordDTO {
    
    private Long id;
    
    @NotBlank(message = "面试类型不能为空")
    private String interviewType;
    
    @NotNull(message = "面试时间不能为空")
    private LocalDateTime interviewTime;
}