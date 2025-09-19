package cn.lazylhxzzy.resume_commit.dto;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 投递记录DTO
 */
@Data
public class ResumeRecordDTO {
    
    private Long id;
    
    @NotBlank(message = "公司名称不能为空")
    private String companyName;
    
    @NotBlank(message = "投递岗位不能为空")
    private String position;
    
    private String baseLocation;
    
    @NotNull(message = "投递时间不能为空")
    private LocalDateTime applyTime;
    
    private LocalDateTime testTime;
    
    private LocalDateTime writtenExamTime;
    
    private String currentStatus;
    
    private LocalDateTime currentStatusDate;
    
    private String finalResult;
    
    private String expectedSalaryType;
    
    private String expectedSalaryValue;
    
    private String remarks;
    
    private String companyUrl;
    
    private String companyGroupId;
    
    private Boolean isPrimary;
    
    // 面试记录列表
    private List<InterviewRecordDTO> interviews;
    
    // 泡池时间（前端计算）
    private Integer poolDays;
    
    private List<PositionInfo> positions; // 同公司的所有岗位信息
    
    private PositionInfo currentPosition; // 当前显示的岗位
    
    // 岗位信息内部类
    public static class PositionInfo {
        private Long id;
        private String position;
        private String finalResult;
        private String currentStatus;
        private LocalDateTime currentStatusDate;
        private Integer poolDays;
        private String expectedSalaryType;
        private String expectedSalaryValue;
        private String remarks;
        private List<InterviewRecordDTO> interviews;
        
        // 流程时间信息
        private LocalDateTime applyTime;
        private LocalDateTime testTime;
        private LocalDateTime writtenExamTime;
        
        // getters and setters
        public Long getId() { return id; }
        public void setId(Long id) { this.id = id; }
        
        public String getPosition() { return position; }
        public void setPosition(String position) { this.position = position; }
        
        public String getFinalResult() { return finalResult; }
        public void setFinalResult(String finalResult) { this.finalResult = finalResult; }
        
        public String getCurrentStatus() { return currentStatus; }
        public void setCurrentStatus(String currentStatus) { this.currentStatus = currentStatus; }
        
        public LocalDateTime getCurrentStatusDate() { return currentStatusDate; }
        public void setCurrentStatusDate(LocalDateTime currentStatusDate) { this.currentStatusDate = currentStatusDate; }
        
        public Integer getPoolDays() { return poolDays; }
        public void setPoolDays(Integer poolDays) { this.poolDays = poolDays; }
        
        public String getExpectedSalaryType() { return expectedSalaryType; }
        public void setExpectedSalaryType(String expectedSalaryType) { this.expectedSalaryType = expectedSalaryType; }
        
        public String getExpectedSalaryValue() { return expectedSalaryValue; }
        public void setExpectedSalaryValue(String expectedSalaryValue) { this.expectedSalaryValue = expectedSalaryValue; }
        
        public String getRemarks() { return remarks; }
        public void setRemarks(String remarks) { this.remarks = remarks; }
        
        public List<InterviewRecordDTO> getInterviews() { return interviews; }
        public void setInterviews(List<InterviewRecordDTO> interviews) { this.interviews = interviews; }
        
        public LocalDateTime getApplyTime() { return applyTime; }
        public void setApplyTime(LocalDateTime applyTime) { this.applyTime = applyTime; }
        
        public LocalDateTime getTestTime() { return testTime; }
        public void setTestTime(LocalDateTime testTime) { this.testTime = testTime; }
        
        public LocalDateTime getWrittenExamTime() { return writtenExamTime; }
        public void setWrittenExamTime(LocalDateTime writtenExamTime) { this.writtenExamTime = writtenExamTime; }
    }
}