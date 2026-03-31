package com.example.recruitment.mapper;

import com.example.recruitment.entity.SysLog;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SysLogMapper {
    int insert(SysLog log);
}

