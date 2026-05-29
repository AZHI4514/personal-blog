package com.azhi.mapper;

import com.azhi.pojo.Image;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface ImageMapper {
    @Select("SELECT id, path, author FROM image ORDER BY create_time DESC")
    List<Image> findAllImages();
}
