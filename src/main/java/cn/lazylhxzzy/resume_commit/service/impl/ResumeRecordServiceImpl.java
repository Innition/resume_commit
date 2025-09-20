package cn.lazylhxzzy.resume_commit.service.impl;

import cn.lazylhxzzy.resume_commit.annotation.BusinessLog;
import cn.lazylhxzzy.resume_commit.dto.InterviewRecordDTO;
import cn.lazylhxzzy.resume_commit.dto.ResumeRecordDTO;
import cn.lazylhxzzy.resume_commit.entity.InterviewRecord;
import cn.lazylhxzzy.resume_commit.entity.ResumeRecord;
import cn.lazylhxzzy.resume_commit.mapper.InterviewRecordMapper;
import cn.lazylhxzzy.resume_commit.mapper.ResumeRecordMapper;
import cn.lazylhxzzy.resume_commit.service.ResumeRecordService;
import cn.lazylhxzzy.resume_commit.util.ExcelUtil;
import cn.lazylhxzzy.resume_commit.util.ExcelImportUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import java.io.InputStream;

/**
 * 投递记录服务实现类
 */
@Service
public class ResumeRecordServiceImpl extends ServiceImpl<ResumeRecordMapper, ResumeRecord> implements ResumeRecordService {
    
    @Autowired
    private ResumeRecordMapper resumeRecordMapper;
    
    @Autowired
    private InterviewRecordMapper interviewRecordMapper;
    
    @Override
    public List<ResumeRecordDTO> getUserRecords(Long userId) {
        QueryWrapper<ResumeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        
        List<ResumeRecord> records = resumeRecordMapper.selectList(queryWrapper);
        
        // 为每个record设置companyGroupId（如果没有的话）
        for (ResumeRecord record : records) {
            if (record.getCompanyGroupId() == null) {
                record.setCompanyGroupId(generateGroupId(record.getUserId(), record.getCompanyName()));
            }
        }
        
        // 按公司分组排序，然后按更新时间排序
        records.sort((r1, r2) -> {
            // 先按公司分组排序
            int groupCompare = r1.getCompanyGroupId().compareTo(r2.getCompanyGroupId());
            if (groupCompare != 0) return groupCompare;
            
            // 同公司内按最终结果排序（OC > PENDING > 其他）
            int resultCompare = compareFinalResults(r1.getFinalResult(), r2.getFinalResult());
            if (resultCompare != 0) return resultCompare;
            
            // 然后按更新时间排序
            return r2.getUpdatedAt().compareTo(r1.getUpdatedAt());
        });
        
        // 转换为DTO
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ResumeRecordDTO> getAllRecords() {
        QueryWrapper<ResumeRecord> queryWrapper = new QueryWrapper<>();
        
        List<ResumeRecord> records = resumeRecordMapper.selectList(queryWrapper);
        
        // 为每个record设置companyGroupId（如果没有的话）
        for (ResumeRecord record : records) {
            if (record.getCompanyGroupId() == null) {
                record.setCompanyGroupId(generateGroupId(record.getUserId(), record.getCompanyName()));
            }
        }
        
        // 按公司分组排序，然后按更新时间排序
        records.sort((r1, r2) -> {
            // 先按公司分组排序
            int groupCompare = r1.getCompanyGroupId().compareTo(r2.getCompanyGroupId());
            if (groupCompare != 0) return groupCompare;
            
            // 同公司内按最终结果排序（OC > PENDING > 其他）
            int resultCompare = compareFinalResults(r1.getFinalResult(), r2.getFinalResult());
            if (resultCompare != 0) return resultCompare;
            
            // 然后按更新时间排序
            return r2.getUpdatedAt().compareTo(r1.getUpdatedAt());
        });
        
        // 转换为DTO
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional
    @BusinessLog(value = "添加投递记录", module = "RESUME", operation = "ADD_RECORD")
    public void addRecord(ResumeRecordDTO recordDTO, Long userId) {
        // 检查是否已存在相同公司的记录
        QueryWrapper<ResumeRecord> existingQuery = new QueryWrapper<>();
        existingQuery.eq("user_id", userId)
                    .eq("company_name", recordDTO.getCompanyName());
        
        List<ResumeRecord> existingRecords = resumeRecordMapper.selectList(existingQuery);
        String companyGroupId;
        
        if (!existingRecords.isEmpty()) {
            // 使用现有记录的company_group_id
            companyGroupId = existingRecords.get(0).getCompanyGroupId();
            if (companyGroupId == null) {
                // 如果现有记录没有group_id，生成一个
                companyGroupId = generateGroupId(userId, recordDTO.getCompanyName());
                // 更新现有记录的group_id
                UpdateWrapper<ResumeRecord> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("user_id", userId)
                            .eq("company_name", recordDTO.getCompanyName())
                            .set("company_group_id", companyGroupId);
                resumeRecordMapper.update(null, updateWrapper);
            }
        } else {
            // 新公司，生成新的group_id
            companyGroupId = generateGroupId(userId, recordDTO.getCompanyName());
        }
        
        // 创建投递记录
        ResumeRecord record = new ResumeRecord();
        BeanUtils.copyProperties(recordDTO, record);
        record.setUserId(userId);
        record.setCompanyGroupId(companyGroupId);
        record.setIsPrimary(false); // 新岗位不是主要岗位
        record.setCreatedAt(LocalDateTime.now());
        record.setUpdatedAt(LocalDateTime.now());
        record.setCurrentStatus(recordDTO.getCurrentStatus());
        resumeRecordMapper.insert(record);
        
        // 添加面试记录
        if (recordDTO.getInterviews() != null && !recordDTO.getInterviews().isEmpty()) {
            for (InterviewRecordDTO interviewDTO : recordDTO.getInterviews()) {
                InterviewRecord interview = new InterviewRecord();
                interview.setResumeRecordId(record.getId());
                interview.setInterviewType(interviewDTO.getInterviewType());
                interview.setInterviewTime(interviewDTO.getInterviewTime());
                interview.setCreatedAt(LocalDateTime.now());
                
                interviewRecordMapper.insert(interview);
            }
        }
    }
    
    @Override
    @Transactional
    @BusinessLog(value = "批量添加投递记录", module = "RESUME", operation = "ADD_MULTIPLE_RECORDS")
    public void addMultipleRecords(List<ResumeRecordDTO> recordDTOs, Long userId) {
        if (recordDTOs == null || recordDTOs.isEmpty()) {
            return;
        }
        
        // 获取公司名称（所有记录应该是同一公司）
        String companyName = recordDTOs.get(0).getCompanyName();
        
        // 检查是否已存在相同公司的记录
        QueryWrapper<ResumeRecord> existingQuery = new QueryWrapper<>();
        existingQuery.eq("user_id", userId)
                    .eq("company_name", companyName);
        
        List<ResumeRecord> existingRecords = resumeRecordMapper.selectList(existingQuery);
        String companyGroupId;
        
        if (!existingRecords.isEmpty()) {
            // 使用现有记录的company_group_id
            companyGroupId = existingRecords.get(0).getCompanyGroupId();
            if (companyGroupId == null) {
                // 如果现有记录没有group_id，生成一个
                companyGroupId = generateGroupId(userId, companyName);
                // 更新现有记录的group_id
                UpdateWrapper<ResumeRecord> updateWrapper = new UpdateWrapper<>();
                updateWrapper.eq("user_id", userId)
                            .eq("company_name", companyName)
                            .set("company_group_id", companyGroupId);
                resumeRecordMapper.update(null, updateWrapper);
            }
        } else {
            // 新公司，生成新的group_id
            companyGroupId = generateGroupId(userId, companyName);
        }
        
        // 批量创建投递记录
        for (int i = 0; i < recordDTOs.size(); i++) {
            ResumeRecordDTO recordDTO = recordDTOs.get(i);
            ResumeRecord record = new ResumeRecord();
            BeanUtils.copyProperties(recordDTO, record);
            record.setUserId(userId);
            record.setCompanyGroupId(companyGroupId); // 使用相同的group_id
            record.setIsPrimary(i == 0); // 第一个岗位作为主要岗位
            record.setCreatedAt(LocalDateTime.now());
            record.setUpdatedAt(LocalDateTime.now());
            
            if(recordDTO.getCurrentStatus() != null)
                if(recordDTO.getCurrentStatus().isEmpty() || recordDTO.getCurrentStatus().isBlank())
                    record.setCurrentStatus(null);
                else record.setCurrentStatus(recordDTO.getCurrentStatus());
            else record.setCurrentStatus(null);
            
            resumeRecordMapper.insert(record);
            
            // 添加面试记录
            if (recordDTO.getInterviews() != null && !recordDTO.getInterviews().isEmpty()) {
                for (InterviewRecordDTO interviewDTO : recordDTO.getInterviews()) {
                    InterviewRecord interview = new InterviewRecord();
                    interview.setResumeRecordId(record.getId());
                    interview.setInterviewType(interviewDTO.getInterviewType());
                    interview.setInterviewTime(interviewDTO.getInterviewTime());
                    interview.setCreatedAt(LocalDateTime.now());
                    
                    interviewRecordMapper.insert(interview);
                }
            }
        }
    }
    
    @Override
    @Transactional
    @BusinessLog(value = "更新投递记录", module = "RESUME", operation = "UPDATE_RECORD")
    public void updateRecord(ResumeRecordDTO recordDTO, Long userId) {
        // 检查记录是否存在且属于当前用户
        ResumeRecord existingRecord = resumeRecordMapper.selectById(recordDTO.getId());
        if (existingRecord == null || !existingRecord.getUserId().equals(userId)) {
            throw new RuntimeException("记录不存在或无权限修改");
        }
        
        // 检查公司名称是否发生变化，如果变化了需要处理company_group_id
        String newCompanyGroupId = existingRecord.getCompanyGroupId();
        if (!existingRecord.getCompanyName().equals(recordDTO.getCompanyName())) {
            // 公司名称发生变化，需要重新分组
            // 检查新公司名称是否已存在
            QueryWrapper<ResumeRecord> newCompanyQuery = new QueryWrapper<>();
            newCompanyQuery.eq("user_id", userId)
                          .eq("company_name", recordDTO.getCompanyName());
            
            List<ResumeRecord> newCompanyRecords = resumeRecordMapper.selectList(newCompanyQuery);
            if (!newCompanyRecords.isEmpty()) {
                // 新公司已存在，使用其group_id
                newCompanyGroupId = newCompanyRecords.get(0).getCompanyGroupId();
                if (newCompanyGroupId == null) {
                    newCompanyGroupId = generateGroupId(userId, recordDTO.getCompanyName());
                    // 更新新公司的所有记录的group_id
                    UpdateWrapper<ResumeRecord> updateNewCompanyWrapper = new UpdateWrapper<>();
                    updateNewCompanyWrapper.eq("user_id", userId)
                                          .eq("company_name", recordDTO.getCompanyName())
                                          .set("company_group_id", newCompanyGroupId);
                    resumeRecordMapper.update(null, updateNewCompanyWrapper);
                }
            } else {
                // 新公司不存在，生成新的group_id
                newCompanyGroupId = generateGroupId(userId, recordDTO.getCompanyName());
            }
        }
        
        // 使用UpdateWrapper来确保null值也能被更新
        UpdateWrapper<ResumeRecord> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("id", recordDTO.getId())
                    .set("company_name", recordDTO.getCompanyName())
                    .set("position", recordDTO.getPosition())
                    .set("base_location", recordDTO.getBaseLocation())
                    .set("company_url", recordDTO.getCompanyUrl())
                    .set("apply_time", recordDTO.getApplyTime())
                    .set("test_time", recordDTO.getTestTime())
                    .set("written_exam_time", recordDTO.getWrittenExamTime())
                    .set("current_status", recordDTO.getCurrentStatus())
                    .set("current_status_date", recordDTO.getCurrentStatusDate())
                    .set("final_result", recordDTO.getFinalResult())
                    .set("expected_salary_type", recordDTO.getExpectedSalaryType())
                    .set("expected_salary_value", recordDTO.getExpectedSalaryValue())
                    .set("remarks", recordDTO.getRemarks())
                    .set("company_group_id", newCompanyGroupId)
                    .set("updated_at", LocalDateTime.now());
        
        resumeRecordMapper.update(null, updateWrapper);
        
        // 删除原有面试记录
        QueryWrapper<InterviewRecord> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("resume_record_id", recordDTO.getId());
        interviewRecordMapper.delete(deleteWrapper);
        
        // 添加新的面试记录
        if (recordDTO.getInterviews() != null && !recordDTO.getInterviews().isEmpty()) {
            for (InterviewRecordDTO interviewDTO : recordDTO.getInterviews()) {
                InterviewRecord interview = new InterviewRecord();
                interview.setResumeRecordId(recordDTO.getId());
                interview.setInterviewType(interviewDTO.getInterviewType());
                interview.setInterviewTime(interviewDTO.getInterviewTime());
                interview.setCreatedAt(LocalDateTime.now());
                
                interviewRecordMapper.insert(interview);
            }
        }
    }
    
    @Override
    @Transactional
    @BusinessLog(value = "删除投递记录", module = "RESUME", operation = "DELETE_RECORD")
    public void deleteRecord(Long recordId, Long userId) {
        // 检查记录是否存在且属于当前用户
        ResumeRecord record = resumeRecordMapper.selectById(recordId);
        if (record == null || !record.getUserId().equals(userId)) {
            throw new RuntimeException("记录不存在或无权限删除");
        }
        
        // 删除面试记录（外键约束会自动删除）
        resumeRecordMapper.deleteById(recordId);
    }
    
    @Override
    public byte[] exportUserRecords(Long userId) {
        try {
            List<ResumeRecordDTO> records = getUserRecords(userId);
            return ExcelUtil.exportResumeRecords(records);
        } catch (Exception e) {
            throw new RuntimeException("导出用户数据失败", e);
        }
    }
    
    @Override
    public byte[] exportAllRecords() {
        try {
            List<ResumeRecordDTO> records = getAllRecords();
            return ExcelUtil.exportResumeRecords(records);
        } catch (Exception e) {
            throw new RuntimeException("导出所有数据失败", e);
        }
    }
    
    @Override
    public byte[] exportSearchUserRecords(Long userId, String keywords, String finalResult, String currentStatus, Double minSalary) {
        try {
            List<ResumeRecordDTO> records = searchUserRecords(userId, keywords, finalResult, currentStatus, minSalary);
            return ExcelUtil.exportResumeRecords(records);
        } catch (Exception e) {
            throw new RuntimeException("导出用户筛选数据失败", e);
        }
    }
    
    @Override
    public byte[] exportSearchAllRecords(String keywords, String finalResult, String currentStatus, Double minSalary) {
        try {
            List<ResumeRecordDTO> records = searchAllRecords(keywords, finalResult, currentStatus, minSalary);
            return ExcelUtil.exportResumeRecords(records);
        } catch (Exception e) {
            throw new RuntimeException("导出筛选数据失败", e);
        }
    }
    
    
    /**
     * 计算泡池时间
     */
    private Integer calculatePoolDays(ResumeRecord record, List<InterviewRecord> interviews) {
        LocalDateTime latestTime = record.getApplyTime();
        
        if (record.getTestTime() != null && record.getTestTime().isAfter(latestTime)) {
            latestTime = record.getTestTime();
        }
        
        if (record.getWrittenExamTime() != null && record.getWrittenExamTime().isAfter(latestTime)) {
            latestTime = record.getWrittenExamTime();
        }
        
        // 考虑当前状态日期
        if (record.getCurrentStatusDate() != null && record.getCurrentStatusDate().isAfter(latestTime)) {
            latestTime = record.getCurrentStatusDate();
        }
        
        for (InterviewRecord interview : interviews) {
            if (interview.getInterviewTime() != null && interview.getInterviewTime().isAfter(latestTime)) {
                latestTime = interview.getInterviewTime();
            }
        }
        
        return (int) java.time.Duration.between(latestTime, LocalDateTime.now()).toDays();
    }
    
    /**
     * 计算泡池时间（接受InterviewRecordDTO列表）
     */
    private Integer calculatePoolDaysFromDTOs(ResumeRecord record, List<InterviewRecordDTO> interviewDTOs) {
        LocalDateTime latestTime = record.getApplyTime();
        
        if (record.getTestTime() != null && record.getTestTime().isAfter(latestTime)) {
            latestTime = record.getTestTime();
        }
        
        if (record.getWrittenExamTime() != null && record.getWrittenExamTime().isAfter(latestTime)) {
            latestTime = record.getWrittenExamTime();
        }
        
        // 考虑当前状态日期
        if (record.getCurrentStatusDate() != null && record.getCurrentStatusDate().isAfter(latestTime)) {
            latestTime = record.getCurrentStatusDate();
        }
        
        for (InterviewRecordDTO interview : interviewDTOs) {
            if (interview.getInterviewTime() != null && interview.getInterviewTime().isAfter(latestTime)) {
                latestTime = interview.getInterviewTime();
            }
        }
        
        return (int) java.time.Duration.between(latestTime, LocalDateTime.now()).toDays();
    }
    
    /**
     * 自定义排序方法
     * 1. OC放在最上面
     * 2. PENDING按泡池时间升序排列
     * 3. 其他结果（简历挂、测评挂、笔试挂、面试挂）放在最下面
     */
    private List<ResumeRecordDTO> sortRecords(List<ResumeRecordDTO> records) {
        return records.stream()
                .sorted((r1, r2) -> {
                    String result1 = r1.getFinalResult();
                    String result2 = r2.getFinalResult();
                    
                    // 1. OC优先级最高
                    if ("OC".equals(result1) && !"OC".equals(result2)) {
                        return -1;
                    }
                    if (!"OC".equals(result1) && "OC".equals(result2)) {
                        return 1;
                    }
                    
                    // 2. PENDING按泡池时间升序排列
                    if ("PENDING".equals(result1) && "PENDING".equals(result2)) {
                        Integer poolDays1 = r1.getPoolDays() != null ? r1.getPoolDays() : 0;
                        Integer poolDays2 = r2.getPoolDays() != null ? r2.getPoolDays() : 0;
                        return poolDays1.compareTo(poolDays2);
                    }
                    
                    // 3. PENDING优先级高于其他结果
                    if ("PENDING".equals(result1) && !"PENDING".equals(result2) && !"OC".equals(result2)) {
                        return -1;
                    }
                    if (!"PENDING".equals(result1) && !"OC".equals(result1) && "PENDING".equals(result2)) {
                        return 1;
                    }
                    
                    // 4. 其他结果（简历挂、测评挂、笔试挂、面试挂）按泡池时间升序排列
                    if (!"OC".equals(result1) && !"PENDING".equals(result1) && 
                        !"OC".equals(result2) && !"PENDING".equals(result2)) {
                        Integer poolDays1 = r1.getPoolDays() != null ? r1.getPoolDays() : 0;
                        Integer poolDays2 = r2.getPoolDays() != null ? r2.getPoolDays() : 0;
                        return poolDays1.compareTo(poolDays2);
                    }
                    
                    // 5. 默认按创建时间降序
                    return 0;
                })
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ResumeRecordDTO> searchUserRecords(Long userId, String keywords, String finalResult, String currentStatus, Double minSalary) {
        QueryWrapper<ResumeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId);
        
        // 应用筛选条件
        applySearchFilters(queryWrapper, keywords, finalResult, currentStatus, minSalary);
        
        List<ResumeRecord> records = resumeRecordMapper.selectList(queryWrapper);
        
        // 为每个record设置companyGroupId（如果没有的话）
        for (ResumeRecord record : records) {
            if (record.getCompanyGroupId() == null) {
                record.setCompanyGroupId(generateGroupId(record.getUserId(), record.getCompanyName()));
            }
        }
        
        // 按公司分组排序，然后按更新时间排序
        records.sort((r1, r2) -> {
            // 先按公司分组排序
            int groupCompare = r1.getCompanyGroupId().compareTo(r2.getCompanyGroupId());
            if (groupCompare != 0) return groupCompare;
            
            // 同公司内按最终结果排序（OC > PENDING > 其他）
            int resultCompare = compareFinalResults(r1.getFinalResult(), r2.getFinalResult());
            if (resultCompare != 0) return resultCompare;
            
            // 然后按更新时间排序
            return r2.getUpdatedAt().compareTo(r1.getUpdatedAt());
        });
        
        // 转换为DTO
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Override
    public List<ResumeRecordDTO> searchAllRecords(String keywords, String finalResult, String currentStatus, Double minSalary) {
        QueryWrapper<ResumeRecord> queryWrapper = new QueryWrapper<>();
        
        // 应用筛选条件
        applySearchFilters(queryWrapper, keywords, finalResult, currentStatus, minSalary);
        
        List<ResumeRecord> records = resumeRecordMapper.selectList(queryWrapper);
        
        // 为每个record设置companyGroupId（如果没有的话）
        for (ResumeRecord record : records) {
            if (record.getCompanyGroupId() == null) {
                record.setCompanyGroupId(generateGroupId(record.getUserId(), record.getCompanyName()));
            }
        }
        
        // 按公司分组排序，然后按更新时间排序
        records.sort((r1, r2) -> {
            // 先按公司分组排序
            int groupCompare = r1.getCompanyGroupId().compareTo(r2.getCompanyGroupId());
            if (groupCompare != 0) return groupCompare;
            
            // 同公司内按最终结果排序（OC > PENDING > 其他）
            int resultCompare = compareFinalResults(r1.getFinalResult(), r2.getFinalResult());
            if (resultCompare != 0) return resultCompare;
            
            // 然后按更新时间排序
            return r2.getUpdatedAt().compareTo(r1.getUpdatedAt());
        });
        
        // 转换为DTO
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 应用搜索和筛选条件
     */
    private void applySearchFilters(QueryWrapper<ResumeRecord> queryWrapper, String keywords, String finalResult, String currentStatus, Double minSalary) {
        // 关键词搜索
        if (keywords != null && !keywords.trim().isEmpty()) {
            String[] keywordArray = keywords.split("[,，]"); // 支持半角和全角逗号
            queryWrapper.and(wrapper -> {
                for (int i = 0; i < keywordArray.length; i++) {
                    String keyword = keywordArray[i].trim();
                    if (!keyword.isEmpty()) {
                        if (i == 0) {
                            wrapper.and(subWrapper -> subWrapper
                                .like("company_name", keyword) // 精确匹配公司名称
                                .or()
                                .like("position", keyword) // 模糊匹配岗位
                                .or()
                                .like("base_location", keyword) // 模糊匹配地点
                            );
                        } else {
                            wrapper.or(subWrapper -> subWrapper
                                .eq("company_name", keyword)
                                .or()
                                .like("position", keyword)
                                .or()
                                .like("base_location", keyword)
                            );
                        }
                    }
                }
            });
        }
        
        // 最终结果筛选
        if (finalResult != null && !finalResult.trim().isEmpty()) {
            if ("已挂".equals(finalResult)) {
                queryWrapper.in("final_result", "简历挂", "测评挂", "笔试挂", "面试挂");
            } else if ("待定".equals(finalResult)) {
                queryWrapper.eq("final_result", "PENDING");
            } else {
                queryWrapper.eq("final_result", finalResult);
            }
        }
        
        // 当前状态筛选
        if (currentStatus != null && !currentStatus.trim().isEmpty()) {
            queryWrapper.eq("current_status", currentStatus);
        }
        
        // 薪资筛选
        if (minSalary != null && minSalary > 0) {
            // 这里需要特殊处理，因为薪资存储格式不同
            // 总包：直接比较数值
            // 月薪：需要计算年包 = 月薪 * 月数
            queryWrapper.and(wrapper -> wrapper
                .apply("(expected_salary_type = '总包' AND CAST(REPLACE(expected_salary_value, 'w', '') AS DECIMAL) >= {0})", minSalary)
                .or()
                .apply("expected_salary_type = '月薪' AND expected_salary_value REGEXP '^[0-9]+(\\.[0-9]+)?k×[0-9]+$' AND " +
                       "CAST(SUBSTRING_INDEX(SUBSTRING_INDEX(expected_salary_value, 'k×', 1), 'k', -1) AS DECIMAL) * " +
                       "CAST(SUBSTRING_INDEX(expected_salary_value, 'k×', -1) AS DECIMAL) >= {0}", minSalary)
            );
        }
    }
    
    /**
     * 生成公司分组ID
     */
    private String generateGroupId(Long userId, String companyName) {
        return "group_" + userId + "_" + companyName + "_" + System.currentTimeMillis();
    }
    
    /**
     * 比较最终结果优先级
     */
    private int compareFinalResults(String result1, String result2) {
        int priority1 = getResultPriority(result1);
        int priority2 = getResultPriority(result2);
        return Integer.compare(priority1, priority2);
    }
    
    /**
     * 获取结果优先级（数字越小优先级越高）
     */
    private int getResultPriority(String result) {
        if (result == null) return 999;
        switch (result) {
            case "OC": return 1;
            case "PENDING": return 2;
            case "简历挂":
            case "测评挂":
            case "笔试挂":
            case "面试挂": return 3;
            default: return 4;
        }
    }
    
    /**
     * 获取同一公司的其他岗位记录（用于编辑时的岗位选择）
     */
    public List<ResumeRecordDTO> getCompanyOtherRecords(Long currentRecordId, String companyName, Long userId) {
        QueryWrapper<ResumeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("user_id", userId)
                   .eq("company_name", companyName)
                   .ne("id", currentRecordId)
                   .orderByDesc("updated_at");
        
        List<ResumeRecord> records = resumeRecordMapper.selectList(queryWrapper);
        
        // 转换为DTO
        return records.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    /**
     * 将ResumeRecord转换为ResumeRecordDTO
     */
    private ResumeRecordDTO convertToDTO(ResumeRecord record) {
        ResumeRecordDTO recordDTO = new ResumeRecordDTO();
        BeanUtils.copyProperties(record, recordDTO);
        
        // 获取面试记录
        QueryWrapper<InterviewRecord> interviewWrapper = new QueryWrapper<>();
        interviewWrapper.eq("resume_record_id", record.getId())
                       .orderByAsc("interview_time");
        List<InterviewRecord> interviews = interviewRecordMapper.selectList(interviewWrapper);
        List<InterviewRecordDTO> interviewDTOs = interviews.stream().map(interview -> {
            InterviewRecordDTO interviewDTO = new InterviewRecordDTO();
            BeanUtils.copyProperties(interview, interviewDTO);
            return interviewDTO;
        }).collect(Collectors.toList());
        recordDTO.setInterviews(interviewDTOs);
        
        // 计算泡池时间
        recordDTO.setPoolDays(calculatePoolDaysFromDTOs(record, interviewDTOs));
        
        return recordDTO;
    }
    
    /**
     * 创建公司分组DTO
     */
    private ResumeRecordDTO createCompanyGroupDTO(List<ResumeRecord> companyRecords, Long userId) {
        if (companyRecords.isEmpty()) {
            return null;
        }
        
        // 使用第一个记录作为基础信息
        ResumeRecord baseRecord = companyRecords.get(0);
        ResumeRecordDTO companyDto = new ResumeRecordDTO();
        
        // 设置公司基本信息
        companyDto.setId(baseRecord.getId());
        companyDto.setCompanyName(baseRecord.getCompanyName());
        companyDto.setBaseLocation(baseRecord.getBaseLocation());
        companyDto.setCompanyUrl(baseRecord.getCompanyUrl());
        
        // 确保companyGroupId不为null，如果为null则生成一个
        String companyGroupId = baseRecord.getCompanyGroupId();
        if (companyGroupId == null) {
            companyGroupId = generateGroupId(userId, baseRecord.getCompanyName());
        }
        companyDto.setCompanyGroupId(companyGroupId);
        
        companyDto.setApplyTime(baseRecord.getApplyTime());
        companyDto.setTestTime(baseRecord.getTestTime());
        companyDto.setWrittenExamTime(baseRecord.getWrittenExamTime());
        
        // 获取所有岗位的面试记录，合并到公司级别
        List<InterviewRecordDTO> allInterviews = new ArrayList<>();
        for (ResumeRecord record : companyRecords) {
            QueryWrapper<InterviewRecord> interviewWrapper = new QueryWrapper<>();
            interviewWrapper.eq("resume_record_id", record.getId())
                           .orderByAsc("interview_time");
            List<InterviewRecord> interviews = interviewRecordMapper.selectList(interviewWrapper);
            List<InterviewRecordDTO> interviewDTOs = interviews.stream().map(interview -> {
                InterviewRecordDTO interviewDTO = new InterviewRecordDTO();
                BeanUtils.copyProperties(interview, interviewDTO);
                return interviewDTO;
            }).collect(Collectors.toList());
            allInterviews.addAll(interviewDTOs);
        }
        // 按时间排序所有面试记录
        allInterviews.sort((a, b) -> {
            if (a.getInterviewTime() == null && b.getInterviewTime() == null) return 0;
            if (a.getInterviewTime() == null) return 1;
            if (b.getInterviewTime() == null) return -1;
            return a.getInterviewTime().compareTo(b.getInterviewTime());
        });
        companyDto.setInterviews(allInterviews);
        
        // 创建岗位信息列表
        List<ResumeRecordDTO.PositionInfo> positions = new ArrayList<>();
        ResumeRecordDTO.PositionInfo currentPosition = null;
        
        for (ResumeRecord record : companyRecords) {
            ResumeRecordDTO.PositionInfo positionInfo = new ResumeRecordDTO.PositionInfo();
            positionInfo.setId(record.getId());
            positionInfo.setPosition(record.getPosition());
            positionInfo.setFinalResult(record.getFinalResult());
            positionInfo.setCurrentStatus(record.getCurrentStatus());
            positionInfo.setCurrentStatusDate(record.getCurrentStatusDate());
            positionInfo.setExpectedSalaryType(record.getExpectedSalaryType());
            positionInfo.setExpectedSalaryValue(record.getExpectedSalaryValue());
            positionInfo.setRemarks(record.getRemarks());
            
            // 为每个岗位设置流程时间信息
            positionInfo.setApplyTime(record.getApplyTime());
            positionInfo.setTestTime(record.getTestTime());
            positionInfo.setWrittenExamTime(record.getWrittenExamTime());
            
            // 获取该岗位的面试记录
            QueryWrapper<InterviewRecord> interviewWrapper = new QueryWrapper<>();
            interviewWrapper.eq("resume_record_id", record.getId())
                           .orderByAsc("interview_time");
            List<InterviewRecord> interviews = interviewRecordMapper.selectList(interviewWrapper);
            List<InterviewRecordDTO> interviewDTOs = interviews.stream().map(interview -> {
                InterviewRecordDTO interviewDTO = new InterviewRecordDTO();
                BeanUtils.copyProperties(interview, interviewDTO);
                return interviewDTO;
            }).collect(Collectors.toList());
            positionInfo.setInterviews(interviewDTOs);
            
            // 计算泡池时间
            positionInfo.setPoolDays(calculatePoolDaysFromDTOs(record, interviewDTOs));
            
            positions.add(positionInfo);
            
            // 设置当前显示岗位（主要岗位或状态最新的）
            if (record.getIsPrimary() != null && record.getIsPrimary()) {
                currentPosition = positionInfo;
            }
        }
        
        // 如果没有设置主要岗位，选择状态最新的
        if (currentPosition == null && !positions.isEmpty()) {
            currentPosition = positions.stream()
                    .max(Comparator.comparing(p -> p.getCurrentStatusDate() != null ? p.getCurrentStatusDate() : 
                         p.getPoolDays() != null ? LocalDateTime.now().minusDays(p.getPoolDays()) : LocalDateTime.MIN))
                    .orElse(positions.get(0));
        }
        
        companyDto.setPositions(positions);
        companyDto.setCurrentPosition(currentPosition);
        
        // 设置当前岗位的基本信息用于显示
        if (currentPosition != null) {
            companyDto.setPosition(currentPosition.getPosition());
            companyDto.setFinalResult(currentPosition.getFinalResult());
            companyDto.setCurrentStatus(currentPosition.getCurrentStatus());
            companyDto.setCurrentStatusDate(currentPosition.getCurrentStatusDate());
            companyDto.setExpectedSalaryType(currentPosition.getExpectedSalaryType());
            companyDto.setExpectedSalaryValue(currentPosition.getExpectedSalaryValue());
            companyDto.setRemarks(currentPosition.getRemarks());
            companyDto.setInterviews(currentPosition.getInterviews());
            companyDto.setPoolDays(currentPosition.getPoolDays());
        }
        
        return companyDto;
    }
    
    @Override
    public List<ResumeRecordDTO> previewImportData(InputStream inputStream, String fileName) {
        try {
            return ExcelImportUtil.parseExcelToRecords(inputStream, fileName);
        } catch (Exception e) {
            throw new RuntimeException("预览导入数据失败", e);
        }
    }
    
    @Override
    @Transactional
    public Map<String, Object> importUserRecords(InputStream inputStream, String fileName, String mode, Long userId) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ResumeRecordDTO> importRecords = ExcelImportUtil.parseExcelToRecords(inputStream, fileName);
            
            if ("replace".equals(mode)) {
                // 替换模式：删除用户所有现有记录
                QueryWrapper<ResumeRecord> deleteWrapper = new QueryWrapper<>();
                deleteWrapper.eq("user_id", userId);
                resumeRecordMapper.delete(deleteWrapper);
            }
            
            int processedCount = 0;
            int successCount = 0;
            int skippedCount = 0;
            
            for (ResumeRecordDTO recordDTO : importRecords) {
                processedCount++;
                
                if ("skip".equals(mode)) {
                    // 跳过模式：检查是否已存在相同公司和岗位的记录
                    QueryWrapper<ResumeRecord> existingQuery = new QueryWrapper<>();
                    existingQuery.eq("user_id", userId)
                                .eq("company_name", recordDTO.getCompanyName())
                                .eq("position", recordDTO.getPosition());
                    
                    ResumeRecord existingRecord = resumeRecordMapper.selectOne(existingQuery);
                    if (existingRecord != null) {
                        skippedCount++;
                        continue;
                    }
                }
                
                try {
                    addRecord(recordDTO, userId);
                    successCount++;
                } catch (Exception e) {
                    System.err.println("导入记录失败: " + e.getMessage());
                    // 继续处理下一条记录
                }
            }
            
            result.put("success", true);
            result.put("processedCount", processedCount);
            result.put("successCount", successCount);
            result.put("skippedCount", skippedCount);
            result.put("message", "导入完成");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        
        return result;
    }
    
    @Override
    @Transactional
    public Map<String, Object> importAllRecords(InputStream inputStream, String fileName, String mode) {
        Map<String, Object> result = new HashMap<>();
        
        try {
            List<ResumeRecordDTO> importRecords = ExcelImportUtil.parseExcelToRecords(inputStream, fileName);
            
            if ("replace".equals(mode)) {
                // 替换模式：删除所有现有记录
                QueryWrapper<ResumeRecord> deleteWrapper = new QueryWrapper<>();
                resumeRecordMapper.delete(deleteWrapper);
            }
            
            int processedCount = 0;
            int successCount = 0;
            int skippedCount = 0;
            
            for (ResumeRecordDTO recordDTO : importRecords) {
                processedCount++;
                
                if ("skip".equals(mode)) {
                    // 跳过模式：检查是否已存在相同公司和岗位的记录
                    QueryWrapper<ResumeRecord> existingQuery = new QueryWrapper<>();
                    existingQuery.eq("company_name", recordDTO.getCompanyName())
                                .eq("position", recordDTO.getPosition());
                    
                    ResumeRecord existingRecord = resumeRecordMapper.selectOne(existingQuery);
                    if (existingRecord != null) {
                        skippedCount++;
                        continue;
                    }
                }
                
                try {
                    // 为ROOT用户导入，需要获取用户ID
                    // 这里简化处理，假设所有记录都属于第一个用户
                    QueryWrapper<ResumeRecord> userQuery = new QueryWrapper<>();
                    userQuery.last("LIMIT 1");
                    ResumeRecord firstRecord = resumeRecordMapper.selectOne(userQuery);
                    Long userId = firstRecord != null ? firstRecord.getUserId() : 1L;
                    
                    addRecord(recordDTO, userId);
                    successCount++;
                } catch (Exception e) {
                    System.err.println("导入记录失败: " + e.getMessage());
                    // 继续处理下一条记录
                }
            }
            
            result.put("success", true);
            result.put("processedCount", processedCount);
            result.put("successCount", successCount);
            result.put("skippedCount", skippedCount);
            result.put("message", "导入完成");
            
        } catch (Exception e) {
            result.put("success", false);
            result.put("message", "导入失败: " + e.getMessage());
        }
        
        return result;
    }
}