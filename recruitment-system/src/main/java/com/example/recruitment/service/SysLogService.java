package com.example.recruitment.service;

import com.example.recruitment.entity.SysLog;

import java.util.List;

/**
 * 操作日志服务接口
 */
public interface SysLogService {

    /**
     * 记录操作日志
     */
    void save(SysLog sysLog);

    /**
     * 分页查询日志列表
     */
    List<SysLog> listByPage(int page, int size);

    /**
     * 查询日志总数
     */
    int getCount();

    /**
     * 按用户名查询日志
     */
    List<SysLog> listByUsername(String username);

    /**
     * 清理指定时间之前的日志
     */
    int cleanBefore(java.time.LocalDateTime before);
    
    /**
     * 多条件分页查询日志
     */
    List<SysLog> listByConditions(String username, String type, 
            java.time.LocalDateTime startTime, java.time.LocalDateTime endTime, 
            int page, int size);
    
    /**
     * 多条件查询日志总数
     */
    int countByConditions(String username, String type, 
            java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);
    
    /**
     * 查询所有符合条件的日志（用于导出）
     */
    List<SysLog> listAllByConditions(String username, String type, 
            java.time.LocalDateTime startTime, java.time.LocalDateTime endTime);
}
