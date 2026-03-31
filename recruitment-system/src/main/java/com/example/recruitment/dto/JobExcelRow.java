package com.example.recruitment.dto;

import com.alibaba.excel.annotation.ExcelProperty;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class JobExcelRow {

    @ExcelProperty(index = 0, value = "公司ID")
    private Long companyId;

    @ExcelProperty(index = 1, value = "岗位名称")
    private String title;

    @ExcelProperty(index = 2, value = "工作城市")
    private String city;

    @ExcelProperty(index = 3, value = "经验要求")
    private String experience;

    @ExcelProperty(index = 4, value = "学历要求")
    private String education;

    @ExcelProperty(index = 5, value = "最低薪资")
    private BigDecimal minSalary;

    @ExcelProperty(index = 6, value = "最高薪资")
    private BigDecimal maxSalary;

    @ExcelProperty(index = 7, value = "薪资单位")
    private String salaryUnit;

    @ExcelProperty(index = 8, value = "技能标签")
    private String skills;

    @ExcelProperty(index = 9, value = "岗位描述")
    private String jobDesc;

    @ExcelProperty(index = 10, value = "发布时间（可选）")
    private String publishTime;
}

