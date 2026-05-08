package com.example.recruitment.mapper;

import com.example.recruitment.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface SysLogMapper {
    int insert(SysLog log);

    List<SysLog> selectByPage();

    int selectCount();

    List<SysLog> selectByUsername(@Param("username") String username);

    int deleteBefore(@Param("before") LocalDateTime before);
    
    List<SysLog> selectByConditions(
            @Param("username") String username,
            @Param("type") String type,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("page") int page,
            @Param("size") int size);
    
    int countByConditions(
            @Param("username") String username,
            @Param("type") String type,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
    
    List<SysLog> selectAllByConditions(
            @Param("username") String username,
            @Param("type") String type,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
