package cn.lazylhxzzy.resume_commit.util;

import cn.lazylhxzzy.resume_commit.dto.ResumeRecordDTO;
import cn.lazylhxzzy.resume_commit.dto.InterviewRecordDTO;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Excel工具类
 */
public class ExcelUtil {
    
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    
    /**
     * 导出投递记录到Excel
     */
    public static byte[] exportResumeRecords(List<ResumeRecordDTO> records) throws IOException {
        Workbook workbook = new XSSFWorkbook();
        Sheet sheet = workbook.createSheet("投递记录");
        
        // 创建标题行 - 按照当前列表显示内容
        Row headerRow = sheet.createRow(0);
        String[] headers = {
            "公司名称", "岗位", "地点", "投递时间", "当前状态", "当前状态日期", 
            "最终结果", "泡池时间", "预期薪资", "流程进度", "备注", "公司链接"
        };
        
        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        
        for (int i = 0; i < headers.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        
        // 填充数据
        int rowNum = 1;
        for (ResumeRecordDTO record : records) {
            // 处理多岗位记录 - 每个岗位一行
            if (record.getPositions() != null && !record.getPositions().isEmpty()) {
                for (ResumeRecordDTO.PositionInfo position : record.getPositions()) {
                    Row row = sheet.createRow(rowNum++);
                    fillRowData(workbook, row, record, position);
                }
            } else {
                // 单岗位记录
                Row row = sheet.createRow(rowNum++);
                fillRowData(workbook, row, record, null);
            }
        }
        
        // 自动调整列宽
        for (int i = 0; i < headers.length; i++) {
            sheet.autoSizeColumn(i);
        }
        
        // 转换为字节数组
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        workbook.write(outputStream);
        workbook.close();
        
        return outputStream.toByteArray();
    }
    
    /**
     * 填充行数据
     */
    private static void fillRowData(Workbook workbook, Row row, ResumeRecordDTO record, ResumeRecordDTO.PositionInfo position) {
        // 使用岗位信息，如果没有则使用记录信息
        String positionName = position != null ? position.getPosition() : record.getPosition();
        String finalResult = position != null ? position.getFinalResult() : record.getFinalResult();
        String currentStatus = position != null ? position.getCurrentStatus() : record.getCurrentStatus();
        String currentStatusDate = position != null && position.getCurrentStatusDate() != null ? 
            position.getCurrentStatusDate().format(DATE_TIME_FORMATTER) : 
            (record.getCurrentStatusDate() != null ? record.getCurrentStatusDate().format(DATE_TIME_FORMATTER) : "");
        String expectedSalaryType = position != null ? position.getExpectedSalaryType() : record.getExpectedSalaryType();
        String expectedSalaryValue = position != null ? position.getExpectedSalaryValue() : record.getExpectedSalaryValue();
        String remarks = position != null ? position.getRemarks() : record.getRemarks();
        Integer poolDays = position != null ? position.getPoolDays() : record.getPoolDays();
        
        // 获取流程时间信息
        String applyTime = position != null && position.getApplyTime() != null ? 
            position.getApplyTime().format(DATE_TIME_FORMATTER) : 
            (record.getApplyTime() != null ? record.getApplyTime().format(DATE_TIME_FORMATTER) : "");
        String testTime = position != null && position.getTestTime() != null ? 
            position.getTestTime().format(DATE_TIME_FORMATTER) : 
            (record.getTestTime() != null ? record.getTestTime().format(DATE_TIME_FORMATTER) : "");
        String writtenExamTime = position != null && position.getWrittenExamTime() != null ? 
            position.getWrittenExamTime().format(DATE_TIME_FORMATTER) : 
            (record.getWrittenExamTime() != null ? record.getWrittenExamTime().format(DATE_TIME_FORMATTER) : "");
        
        // 获取面试记录
        List<InterviewRecordDTO> interviews = position != null ? position.getInterviews() : record.getInterviews();
        
        // 填充数据
        row.createCell(0).setCellValue(record.getCompanyName() != null ? record.getCompanyName() : "");
        row.createCell(1).setCellValue(positionName != null ? positionName : "");
        row.createCell(2).setCellValue(record.getBaseLocation() != null ? record.getBaseLocation() : "");
        row.createCell(3).setCellValue(applyTime);
        row.createCell(4).setCellValue(currentStatus != null ? currentStatus : "");
        row.createCell(5).setCellValue(currentStatusDate);
        row.createCell(6).setCellValue(finalResult != null ? finalResult : "");
        row.createCell(7).setCellValue(poolDays != null ? poolDays.toString() + "天" : "0天");
        
        // 预期薪资
        String salaryInfo = "";
        if (expectedSalaryType != null && expectedSalaryValue != null) {
            if ("总包".equals(expectedSalaryType)) {
                salaryInfo = expectedSalaryValue + "w";
            } else if ("月薪".equals(expectedSalaryType)) {
                salaryInfo = expectedSalaryValue;
            } else if ("待商议".equals(expectedSalaryType)) {
                salaryInfo = "待商议";
            }
        }
        row.createCell(8).setCellValue(salaryInfo);
        
        // 流程进度
        StringBuilder timelineSteps = new StringBuilder();
        if (currentStatus != null && !currentStatusDate.isEmpty()) {
            // 如果有当前状态，只显示投递-当前状态
            if (!applyTime.isEmpty()) {
                timelineSteps.append("投递");
            }
            timelineSteps.append(" → ").append(currentStatus);
        } else {
            // 显示所有填写的流程步骤
            if (!applyTime.isEmpty()) {
                timelineSteps.append("投递");
            }
            if (!testTime.isEmpty()) {
                if (timelineSteps.length() > 0) timelineSteps.append(" → ");
                timelineSteps.append("测评");
            }
            if (!writtenExamTime.isEmpty()) {
                if (timelineSteps.length() > 0) timelineSteps.append(" → ");
                timelineSteps.append("笔试");
            }
            if (interviews != null && !interviews.isEmpty()) {
                List<InterviewRecordDTO> sortedInterviews = interviews.stream()
                    .filter(interview -> interview.getInterviewTime() != null)
                    .sorted((a, b) -> a.getInterviewTime().compareTo(b.getInterviewTime()))
                    .collect(java.util.stream.Collectors.toList());
                
                for (InterviewRecordDTO interview : sortedInterviews) {
                    if (timelineSteps.length() > 0) timelineSteps.append(" → ");
                    timelineSteps.append(interview.getInterviewType());
                }
            }
            if ("OC".equals(finalResult)) {
                if (timelineSteps.length() > 0) timelineSteps.append(" → ");
                timelineSteps.append("Offer");
            }
        }
        row.createCell(9).setCellValue(timelineSteps.toString());
        
        row.createCell(10).setCellValue(remarks != null ? remarks : "");
        row.createCell(11).setCellValue(record.getCompanyUrl() != null ? record.getCompanyUrl() : "");
    }
}