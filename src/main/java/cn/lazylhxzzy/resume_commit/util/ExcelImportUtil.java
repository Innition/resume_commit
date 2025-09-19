package cn.lazylhxzzy.resume_commit.util;

import cn.lazylhxzzy.resume_commit.dto.ResumeRecordDTO;
import cn.lazylhxzzy.resume_commit.dto.InterviewRecordDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Excel导入工具类
 */
public class ExcelImportUtil {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 解析Excel文件为投递记录列表
     */
    public static List<ResumeRecordDTO> parseExcelToRecords(InputStream inputStream, String fileName) throws IOException {
        Workbook workbook = null;
        try {
            // 根据文件扩展名选择工作簿类型
            if (fileName.toLowerCase().endsWith(".xlsx")) {
                workbook = new XSSFWorkbook(inputStream);
            } else if (fileName.toLowerCase().endsWith(".xls")) {
                workbook = new HSSFWorkbook(inputStream);
            } else {
                throw new IllegalArgumentException("不支持的文件格式，请使用.xlsx或.xls文件");
            }
            
            Sheet sheet = workbook.getSheetAt(0);
            List<ResumeRecordDTO> records = new ArrayList<>();
            
            // 跳过标题行，从第二行开始读取
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;
                
                try {
                    ResumeRecordDTO record = parseRowToRecord(row);
                    if (record != null) {
                        records.add(record);
                    }
                } catch (Exception e) {
                    System.err.println("解析第" + (i + 1) + "行数据时出错: " + e.getMessage());
                    // 继续处理下一行
                }
            }
            
            return records;
        } finally {
            if (workbook != null) {
                workbook.close();
            }
        }
    }
    
    /**
     * 解析单行数据为投递记录
     */
    private static ResumeRecordDTO parseRowToRecord(Row row) {
        ResumeRecordDTO record = new ResumeRecordDTO();
        
        // 公司名称（必填）
        String companyName = getCellValueAsString(row.getCell(0));
        if (companyName == null || companyName.trim().isEmpty()) {
            return null; // 跳过空行
        }
        record.setCompanyName(companyName.trim());
        
        // 岗位（必填）
        String position = getCellValueAsString(row.getCell(1));
        if (position == null || position.trim().isEmpty()) {
            return null; // 跳过空行
        }
        record.setPosition(position.trim());
        
        // 地点
        record.setBaseLocation(getCellValueAsString(row.getCell(2)));
        
        // 投递时间（必填）
        String applyTimeStr = getCellValueAsString(row.getCell(3));
        if (applyTimeStr != null && !applyTimeStr.trim().isEmpty()) {
            record.setApplyTime(parseDateTime(applyTimeStr.trim()));
        }
        
        // 当前状态
        record.setCurrentStatus(getCellValueAsString(row.getCell(4)));
        
        // 当前状态日期
        String currentStatusDateStr = getCellValueAsString(row.getCell(5));
        if (currentStatusDateStr != null && !currentStatusDateStr.trim().isEmpty()) {
            record.setCurrentStatusDate(parseDateTime(currentStatusDateStr.trim()));
        }
        
        // 最终结果
        record.setFinalResult(getCellValueAsString(row.getCell(6)));
        
        // 泡池时间（从Excel读取，但会在后端重新计算）
        String poolDaysStr = getCellValueAsString(row.getCell(7));
        if (poolDaysStr != null && poolDaysStr.contains("天")) {
            try {
                int poolDays = Integer.parseInt(poolDaysStr.replace("天", "").trim());
                record.setPoolDays(poolDays);
            } catch (NumberFormatException e) {
                // 忽略解析错误
            }
        }
        
        // 预期薪资
        String salaryInfo = getCellValueAsString(row.getCell(8));
        if (salaryInfo != null && !salaryInfo.trim().isEmpty()) {
            parseSalaryInfo(record, salaryInfo.trim());
        }
        
        // 流程进度（从Excel读取，但会在后端重新解析）
        String timelineStr = getCellValueAsString(row.getCell(9));
        if (timelineStr != null && !timelineStr.trim().isEmpty()) {
            // 解析流程进度，提取面试信息
            parseTimelineInfo(record, timelineStr.trim());
        }
        
        // 备注
        record.setRemarks(getCellValueAsString(row.getCell(10)));
        
        // 公司链接
        record.setCompanyUrl(getCellValueAsString(row.getCell(11)));
        
        return record;
    }
    
    /**
     * 获取单元格值作为字符串
     */
    private static String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        
        switch (cell.getCellType()) {
            case STRING:
                return cell.getStringCellValue();
            case NUMERIC:
                if (DateUtil.isCellDateFormatted(cell)) {
                    return cell.getDateCellValue().toString();
                } else {
                    return String.valueOf((long) cell.getNumericCellValue());
                }
            case BOOLEAN:
                return String.valueOf(cell.getBooleanCellValue());
            case FORMULA:
                return cell.getCellFormula();
            default:
                return null;
        }
    }
    
    /**
     * 解析日期时间字符串
     * 支持多种格式：
     * - yyyy/mm/dd
     * - mm/dd
     * - mm/dd hh:mm
     * - mm/dd hh:mm:ss
     * 分隔符支持：/, :, -, 空格
     */
    private static LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.trim().isEmpty()) {
            return null;
        }
        
        String trimmed = dateTimeStr.trim();
        LocalDateTime now = LocalDateTime.now();
        
        try {
            // 统一分隔符为 '-'
            String normalized = trimmed.replaceAll("[/:]", "-").replaceAll("\\s+", " ");
            
            // 分离日期和时间部分
            String[] dateTimeParts = normalized.split(" ");
            String datePart = dateTimeParts[0];
            String timePart = dateTimeParts.length > 1 ? dateTimeParts[1] : "12:00:00";
            
            // 解析日期部分
            String[] dateComponents = datePart.split("-");
            int year, month, day;
            
            if (dateComponents.length == 3) {
                // yyyy-mm-dd 格式
                year = Integer.parseInt(dateComponents[0]);
                month = Integer.parseInt(dateComponents[1]);
                day = Integer.parseInt(dateComponents[2]);
            } else if (dateComponents.length == 2) {
                // mm-dd 格式，年份默认为今年
                year = now.getYear();
                month = Integer.parseInt(dateComponents[0]);
                day = Integer.parseInt(dateComponents[1]);
            } else {
                System.err.println("无法解析日期格式: " + dateTimeStr);
                return null;
            }
            
            // 解析时间部分
            String[] timeComponents = timePart.split(":");
            int hour, minute, second;
            
            if (timeComponents.length >= 3) {
                // hh:mm:ss 格式
                hour = Integer.parseInt(timeComponents[0]);
                minute = Integer.parseInt(timeComponents[1]);
                second = Integer.parseInt(timeComponents[2]);
            } else if (timeComponents.length == 2) {
                // hh:mm 格式，秒默认为0
                hour = Integer.parseInt(timeComponents[0]);
                minute = Integer.parseInt(timeComponents[1]);
                second = 0;
            } else if (timeComponents.length == 1) {
                // hh 格式，分秒默认为0
                hour = Integer.parseInt(timeComponents[0]);
                minute = 0;
                second = 0;
            } else {
                // 默认时间
                hour = 12;
                minute = 0;
                second = 0;
            }
            
            // 验证日期有效性
            if (month < 1 || month > 12) {
                System.err.println("月份无效: " + month);
                return null;
            }
            if (day < 1 || day > 31) {
                System.err.println("日期无效: " + day);
                return null;
            }
            if (hour < 0 || hour > 23) {
                System.err.println("小时无效: " + hour);
                return null;
            }
            if (minute < 0 || minute > 59) {
                System.err.println("分钟无效: " + minute);
                return null;
            }
            if (second < 0 || second > 59) {
                System.err.println("秒钟无效: " + second);
                return null;
            }
            
            return LocalDateTime.of(year, month, day, hour, minute, second);
            
        } catch (NumberFormatException e) {
            System.err.println("日期时间格式错误: " + dateTimeStr + " - " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("解析日期时间时发生未知错误: " + dateTimeStr + " - " + e.getMessage());
            return null;
        }
    }
    
    /**
     * 解析薪资信息
     */
    private static void parseSalaryInfo(ResumeRecordDTO record, String salaryInfo) {
        if (salaryInfo.equals("待商议")) {
            record.setExpectedSalaryType("待商议");
            record.setExpectedSalaryValue("待商议");
        } else if (salaryInfo.endsWith("w")) {
            record.setExpectedSalaryType("总包");
            record.setExpectedSalaryValue(salaryInfo.replace("w", ""));
        } else if (salaryInfo.contains("k×")) {
            record.setExpectedSalaryType("月薪");
            record.setExpectedSalaryValue(salaryInfo);
        } else {
            // 默认为总包
            record.setExpectedSalaryType("总包");
            record.setExpectedSalaryValue(salaryInfo);
        }
    }
    
    /**
     * 解析流程进度信息，提取面试记录
     */
    private static void parseTimelineInfo(ResumeRecordDTO record, String timelineStr) {
        if (timelineStr == null || timelineStr.trim().isEmpty()) {
            return;
        }
        
        // 解析流程步骤，提取面试信息
        String[] steps = timelineStr.split(" → ");
        List<InterviewRecordDTO> interviews = new ArrayList<>();
        
        for (String step : steps) {
            step = step.trim();
            if (step.matches("(AI面|一面|二面|三面|四面|五面|六面|七面|八面|九面|十面)")) {
                InterviewRecordDTO interview = new InterviewRecordDTO();
                interview.setInterviewType(step);
                // 面试时间暂时设为null，因为Excel中没有具体时间
                interview.setInterviewTime(null);
                interviews.add(interview);
            }
        }
        
        if (!interviews.isEmpty()) {
            record.setInterviews(interviews);
        }
    }
}
