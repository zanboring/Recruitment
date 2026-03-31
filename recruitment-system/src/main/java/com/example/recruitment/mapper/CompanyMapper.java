package com.example.recruitment.mapper;

import com.example.recruitment.entity.Company;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CompanyMapper {
    int insert(Company company);

    int update(Company company);

    int deleteById(Long id);

    Company selectById(Long id);

    List<Company> selectAll();
}

