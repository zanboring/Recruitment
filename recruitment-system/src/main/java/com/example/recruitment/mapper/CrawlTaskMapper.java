package com.example.recruitment.mapper;

import com.example.recruitment.entity.CrawlTask;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CrawlTaskMapper {
    int insert(CrawlTask task);

    int update(CrawlTask task);

    CrawlTask selectById(Long id);

    List<CrawlTask> selectAll();
}

