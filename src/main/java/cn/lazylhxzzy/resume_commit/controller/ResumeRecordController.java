package cn.lazylhxzzy.resume_commit.controller;

import cn.lazylhxzzy.resume_commit.dto.ResumeRecordDTO;
import cn.lazylhxzzy.resume_commit.entity.User;
import cn.lazylhxzzy.resume_commit.mapper.UserMapper;
import cn.lazylhxzzy.resume_commit.service.ResumeRecordService;
import cn.lazylhxzzy.resume_commit.util.JwtUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.InputStream;

/**
 * 投递记录控制器
 */
@Tag(name = "投递记录管理", description = "投递记录的增删改查和导出功能")
@RestController
@RequestMapping("/records")
@CrossOrigin(origins = "*")
public class ResumeRecordController {
    
    @Autowired
    private ResumeRecordService resumeRecordService;
    
    @Autowired
    private UserMapper userMapper;
    
    @Autowired
    private JwtUtil jwtUtil;
    
    @Operation(summary = "获取投递记录", description = "获取当前用户的投递记录列表")
    @GetMapping
    public ResponseEntity<Map<String, Object>> getRecords(@RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            String role = jwtUtil.getRoleFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            List<ResumeRecordDTO> records;
            if ("ROOT".equals(role)) {
                records = resumeRecordService.getAllRecords();
            } else {
                records = resumeRecordService.getUserRecords(user.getId());
            }
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("data", records);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "添加投递记录", description = "添加新的投递记录")
    @PostMapping
    public ResponseEntity<Map<String, Object>> addRecord(@RequestBody ResumeRecordDTO recordDTO, 
                                                        @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            resumeRecordService.addRecord(recordDTO, user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "添加成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "批量添加投递记录", description = "批量添加同一公司的多个岗位记录")
    @PostMapping("/batch")
    public ResponseEntity<Map<String, Object>> addMultipleRecords(@RequestBody List<ResumeRecordDTO> recordDTOs, 
                                                                 @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            resumeRecordService.addMultipleRecords(recordDTOs, user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "批量添加成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "更新投递记录", description = "更新指定的投递记录")
    @PutMapping("/{id}")
    public ResponseEntity<Map<String, Object>> updateRecord(@PathVariable Long id, 
                                                           @RequestBody ResumeRecordDTO recordDTO,
                                                           @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            recordDTO.setId(id);
            resumeRecordService.updateRecord(recordDTO, user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "更新成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "删除投递记录", description = "删除指定的投递记录")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Object>> deleteRecord(@PathVariable Long id,
                                                           @RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            resumeRecordService.deleteRecord(id, user.getId());
            
            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "删除成功");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            Map<String, Object> response = new HashMap<>();
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "导出数据", description = "导出投递记录数据为Excel文件")
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportRecords(@RequestHeader("Authorization") String token) {
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            String role = jwtUtil.getRoleFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            byte[] excelData;
            String filename;
            
            if ("ROOT".equals(role)) {
                excelData = resumeRecordService.exportAllRecords();
                filename = "all_resume_records.xlsx";
            } else {
                excelData = resumeRecordService.exportUserRecords(user.getId());
                filename = "my_resume_records.xlsx";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(summary = "搜索和筛选记录", description = "根据关键词、最终结果、当前状态、最低薪资搜索和筛选记录")
    @GetMapping("/search")
    public ResponseEntity<Map<String, Object>> searchRecords(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String finalResult,
            @RequestParam(required = false) String currentStatus,
            @RequestParam(required = false) Double minSalary) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            String role = jwtUtil.getRoleFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            List<ResumeRecordDTO> records;
            
            if ("ROOT".equals(role)) {
                records = resumeRecordService.searchAllRecords(keywords, finalResult, currentStatus, minSalary);
            } else {
                records = resumeRecordService.searchUserRecords(user.getId(), keywords, finalResult, currentStatus, minSalary);
            }
            
            response.put("success", true);
            response.put("data", records);
            response.put("message", "搜索成功");
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
    
    @Operation(summary = "导出搜索筛选结果", description = "导出搜索筛选结果为Excel文件")
    @GetMapping("/export/search")
    public ResponseEntity<byte[]> exportSearchRecords(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) String keywords,
            @RequestParam(required = false) String finalResult,
            @RequestParam(required = false) String currentStatus,
            @RequestParam(required = false) Double minSalary) {
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            String role = jwtUtil.getRoleFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            if (user == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            byte[] excelData;
            String filename;
            
            if ("ROOT".equals(role)) {
                excelData = resumeRecordService.exportSearchAllRecords(keywords, finalResult, currentStatus, minSalary);
                filename = "search_resume_records.xlsx";
            } else {
                excelData = resumeRecordService.exportSearchUserRecords(user.getId(), keywords, finalResult, currentStatus, minSalary);
                filename = "my_search_resume_records.xlsx";
            }
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.setContentDispositionFormData("attachment", filename);
            
            return new ResponseEntity<>(excelData, headers, HttpStatus.OK);
        } catch (Exception e) {
            return ResponseEntity.badRequest().build();
        }
    }
    
    @Operation(summary = "导入Excel数据", description = "导入Excel文件数据到系统")
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importRecords(
            @RequestHeader("Authorization") String token,
            @RequestParam("file") MultipartFile file,
            @RequestParam("mode") String mode) {
        
        Map<String, Object> response = new HashMap<>();
        
        try {
            String username = jwtUtil.getUsernameFromToken(token.substring(7));
            String role = jwtUtil.getRoleFromToken(token.substring(7));
            
            // 获取用户ID
            User user = userMapper.selectOne(
                new QueryWrapper<User>()
                    .eq("username", username)
            );
            
            if (user == null) {
                response.put("success", false);
                response.put("message", "用户不存在");
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
            }
            
            // 验证文件
            if (file.isEmpty()) {
                response.put("success", false);
                response.put("message", "请选择要导入的文件");
                return ResponseEntity.badRequest().body(response);
            }
            
            String fileName = file.getOriginalFilename();
            if (fileName == null || (!fileName.toLowerCase().endsWith(".xlsx") && !fileName.toLowerCase().endsWith(".xls"))) {
                response.put("success", false);
                response.put("message", "请选择Excel文件（.xlsx或.xls格式）");
                return ResponseEntity.badRequest().body(response);
            }
            
            InputStream inputStream = file.getInputStream();
            
            if ("preview".equals(mode)) {
                // 预览模式
                List<ResumeRecordDTO> records = resumeRecordService.previewImportData(inputStream, fileName);
                response.put("success", true);
                response.put("data", records);
                response.put("message", "预览成功");
            } else {
                // 导入模式
                Map<String, Object> importResult;
                if ("ROOT".equals(role)) {
                    importResult = resumeRecordService.importAllRecords(inputStream, fileName, mode);
                } else {
                    importResult = resumeRecordService.importUserRecords(inputStream, fileName, mode, user.getId());
                }
                
                response.putAll(importResult);
            }
            
            return ResponseEntity.ok(response);
            
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "导入失败: " + e.getMessage());
            return ResponseEntity.badRequest().body(response);
        }
    }
}