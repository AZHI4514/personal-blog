package com.azhi.mapper;

import com.azhi.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface UserMapper {
    @Insert("INSERT INTO user (username, password, email) VALUES (#{username}, #{password}, #{email})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(User user);

    @Select("SELECT id, username, password, email FROM user WHERE username = #{username}")
    User selectByUsername(String username);

    @Select("SELECT id, username, password, email FROM user WHERE email = #{email}")
    User selectByEmail(String email);
}
