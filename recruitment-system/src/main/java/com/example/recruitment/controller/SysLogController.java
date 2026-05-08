package com.example.recruitment.controller;

import com.example.recruitment.annotation.Log;
import com.example.recruitment.common.Result;
import com.example.recruitment.entity.SysLog;
import com.example.recruitment.service.SysLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 操作日志管理接口
 */
@Slf4j
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Tag(name = "操作日志", description = "系统操作日志查询与管理")
public class SysLogController {

    private final SysLogService sysLogService;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 分页查询操作日志（管理员权限）
     */
    @GetMapping("/list")
    @Operation(summary = "日志列表", description = "分页查询系统操作日志（支持多条件筛选）")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Map<String, Object>> getList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        
        int currentPage = pageNum != null ? pageNum : page;
        int pageSizeValue = pageSize != null ? pageSize : size;
        int offset = (currentPage - 1) * pageSizeValue;
        
        List<SysLog> list = sysLogService.listByConditions(username, type, start, end, offset, pageSizeValue);
        int total = sysLogService.countByConditions(username, type, start, end);

        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", total);
        data.put("pageNum", currentPage);
        data.put("pageSize", pageSizeValue);

        return Result.success(data);
    }

    /**
     * 导出操作日志为Excel（管理员权限）
     */
    @GetMapping("/export")
    @Operation(summary = "导出日志", description = "导出操作日志为Excel文件")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String username,
            @RequestParam(required = false) String type,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) throws IOException {
        
        LocalDateTime start = parseDateTime(startTime);
        LocalDateTime end = parseDateTime(endTime);
        
        List<SysLog> logs = sysLogService.listAllByConditions(username, type, start, end);
        
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("操作日志");
            
            Row headerRow = sheet.createRow(0);
            String[] headers = {"日志ID", "操作人", "操作类型", "操作描述", "请求方法", "请求路径", "客户端IP", "操作时间", "执行结果"};
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }
            
            int rowNum = 1;
            for (SysLog logItem : logs) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(logItem.getId());
                row.createCell(1).setCellValue(logItem.getUsername() != null ? logItem.getUsername() : "未知");
                row.createCell(2).setCellValue(logItem.getAction() != null ? logItem.getAction() : "-");
                row.createCell(3).setCellValue(logItem.getParams() != null ? logItem.getParams() : "-");
                row.createCell(4).setCellValue(logItem.getMethod() != null ? logItem.getMethod() : "-");
                row.createCell(5).setCellValue(logItem.getUri() != null ? logItem.getUri() : "-");
                row.createCell(6).setCellValue(logItem.getIp() != null ? logItem.getIp() : "-");
                row.createCell(7).setCellValue(logItem.getCreatedAt() != null ? logItem.getCreatedAt().format(DATE_FORMATTER) : "-");
                row.createCell(8).setCellValue(logItem.getSuccess() ? "成功" : "失败");
            }
            
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }
            
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            workbook.write(outputStream);
            
            org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
            httpHeaders.add("Content-Disposition", "attachment; filename=操作日志.xlsx");
            httpHeaders.add("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
            
            return new ResponseEntity<>(outputStream.toByteArray(), httpHeaders, org.springframework.http.HttpStatus.OK);
        }
    }

    /**
     * 按用户名查询操作日志（管理员权限）
     */
    @GetMapping("/user/{username}")
    @Operation(summary = "按用户查询日志", description = "按用户名查询操作日志")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<List<SysLog>> getByUsername(@PathVariable String username) {
        return Result.success(sysLogService.listByUsername(username));
    }

    /**
     * 清理历史日志（管理员权限）
     */
    @DeleteMapping("/clean")
    @Operation(summary = "清理历史日志", description = "清理指定天数之前的操作日志")
    @PreAuthorize("hasRole('ADMIN')")
    @Log("清理历史日志")
    public Result<Integer> clean(@RequestParam(defaultValue = "30") int days) {
        LocalDateTime before = LocalDateTime.now().minusDays(days);
        int count = sysLogService.cleanBefore(before);
        log.info("清理历史日志: 删除{}天前的日志, 共{}条", days, count);
        return Result.success(count);
    }
    
    private LocalDateTime parseDateTime(String dateTimeStr) {
        if (dateTimeStr == null || dateTimeStr.isEmpty()) {
            return null;
        }
        try {
            return LocalDateTime.parse(dateTimeStr);
        } catch (Exception e) {
            try {
                // 前端 datetime-local 可能缺少秒部分，补上后重试
                return LocalDateTime.parse(dateTimeStr + ":00");
            } catch (Exception e2) {
                return null;
            }
        }
    }
}
