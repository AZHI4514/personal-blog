package com.azhi.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ClapMapper {
    @Update("UPDATE clap SET total = total + 1 WHERE id = 1")
    void incrementClap();
}
