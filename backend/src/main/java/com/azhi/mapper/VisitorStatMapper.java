package com.azhi.mapper;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface VisitorStatMapper {
    @Select("SELECT COALESCE(SUM(total), 0) FROM visitor_stat")
    Long getTotalVisitors();

    @Insert("INSERT INTO visitor_stat (ip, total, update_time) VALUES (#{ip}, 1, NOW()) " +
            "ON DUPLICATE KEY UPDATE total = total + 1, update_time = NOW()")
    int recordVisitor(@Param("ip") String ip);
}
