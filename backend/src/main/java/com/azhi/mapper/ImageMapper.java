package com.azhi.mapper;

import com.azhi.pojo.Image;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Select;

import java.util.List;


@Mapper
public interface ImageMapper {
    @Select("SELECT id, path, author FROM image ORDER BY create_time DESC")
    List<Image> findAllImages();

    @Select("SELECT id, path, author FROM image WHERE id = #{imageId}")
    Image findImageById(Long imageId);

    @Insert("INSERT INTO image (path, author) VALUES (#{path}, #{author})")
    void insertImage(Image image);

    @Delete("DELETE FROM image WHERE id = #{imageId}")
    void deleteImageById(Long imageId);
}
