package com.example.recruitment.mapper;

import com.example.recruitment.entity.User;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface UserMapper {

    User findByUsername(@Param("username") String username);

    User selectById(@Param("id") Long id);

    int insert(User user);

    int update(User user);
    
    List<User> listUsers(@Param("username") String username, @Param("offset") Integer offset, @Param("limit") Integer limit);
    
    long countUsers();
    
    int deleteById(@Param("id") Long id);
}

