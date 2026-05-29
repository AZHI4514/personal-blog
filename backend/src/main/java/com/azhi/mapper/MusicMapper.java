package com.azhi.mapper;

import com.azhi.pojo.Music;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface MusicMapper {
    @Select("SELECT id, title, artist, file_path, cover_path FROM music ORDER BY update_time DESC")
    List<Music> findAllMusics();
}
