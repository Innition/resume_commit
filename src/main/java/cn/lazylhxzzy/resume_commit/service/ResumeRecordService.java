package cn.lazylhxzzy.resume_commit.service;

import cn.lazylhxzzy.resume_commit.dto.ResumeRecordDTO;
import cn.lazylhxzzy.resume_commit.entity.ResumeRecord;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;
import java.util.Map;
import java.io.InputStream;

/**
 * 投递记录服务接口
 */
public interface ResumeRecordService extends IService<ResumeRecord> {
    
    /**
     * 获取用户的投递记录
     */
    List<ResumeRecordDTO> getUserRecords(Long userId);
    
    /**
     * 获取所有投递记录（ROOT用户）
     */
    List<ResumeRecordDTO> getAllRecords();
    
    /**
     * 添加投递记录
     */
    void addRecord(ResumeRecordDTO recordDTO, Long userId);
    
    /**
     * 批量添加投递记录（同一公司的多个岗位）
     */
    void addMultipleRecords(List<ResumeRecordDTO> recordDTOs, Long userId);
    
    /**
     * 更新投递记录
     */
    void updateRecord(ResumeRecordDTO recordDTO, Long userId);
    
    /**
     * 删除投递记录
     */
    void deleteRecord(Long recordId, Long userId);
    
    /**
     * 导出用户数据为Excel
     */
    byte[] exportUserRecords(Long userId);
    
    /**
     * 导出所有数据为Excel（ROOT用户）
     */
    byte[] exportAllRecords();
    
    /**
     * 导入Excel数据（预览模式）
     */
    List<ResumeRecordDTO> previewImportData(InputStream inputStream, String fileName);
    
    /**
     * 导入Excel数据（用户）
     */
    Map<String, Object> importUserRecords(InputStream inputStream, String fileName, String mode, Long userId);
    
    /**
     * 导入Excel数据（ROOT用户）
     */
    Map<String, Object> importAllRecords(InputStream inputStream, String fileName, String mode);
}