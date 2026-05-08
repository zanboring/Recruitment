package com.example.recruitment.service.impl;

import com.example.recruitment.entity.SysLog;
import com.example.recruitment.mapper.SysLogMapper;
import com.example.recruitment.service.SysLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 操作日志服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SysLogServiceImpl implements SysLogService {

    private final SysLogMapper sysLogMapper;

    @Async
    @Override
    public void save(SysLog sysLog) {
        try {
            if (sysLog.getCreatedAt() == null) {
                sysLog.setCreatedAt(LocalDateTime.now());
            }
            sysLogMapper.insert(sysLog);
        } catch (Exception e) {
            log.error("保存操作日志失败: {}", e.getMessage(), e);
        }
    }

    @Override
    public List<SysLog> listByPage(int page, int size) {
        return sysLogMapper.selectByPage();
    }

    @Override
    public int getCount() {
        return sysLogMapper.selectCount();
    }

    @Override
    public List<SysLog> listByUsername(String username) {
        return sysLogMapper.selectByUsername(username);
    }

    @Override
    public int cleanBefore(LocalDateTime before) {
        return sysLogMapper.deleteBefore(before);
    }
    
    @Override
    public List<SysLog> listByConditions(String username, String type, 
            LocalDateTime startTime, LocalDateTime endTime, int page, int size) {
        return sysLogMapper.selectByConditions(username, type, startTime, endTime, page, size);
    }
    
    @Override
    public int countByConditions(String username, String type, 
            LocalDateTime startTime, LocalDateTime endTime) {
        return sysLogMapper.countByConditions(username, type, startTime, endTime);
    }
    
    @Override
    public List<SysLog> listAllByConditions(String username, String type, 
            LocalDateTime startTime, LocalDateTime endTime) {
        return sysLogMapper.selectAllByConditions(username, type, startTime, endTime);
    }
}
