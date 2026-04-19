package com.example.recruitment.mapper;

import com.example.recruitment.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    int insert(User user);

    int update(User user);
}

