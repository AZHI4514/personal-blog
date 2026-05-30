package com.azhi.mapper;

import com.azhi.pojo.Post;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface PostMapper {

    /**
     * 插入新帖子（主帖或回复）
     * @param post 帖子对象（id 为 null，数据库自增）
     * @return 影响行数
     */
    @Insert("INSERT INTO post (parent_id, username, email, title, content, image_path, delete_key, create_time, update_time) " +
            "VALUES (#{parentId}, #{username}, #{email}, #{title}, #{content}, #{imagePath}, #{deleteKey}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(Post post);

    /**
     * 查询所有顶级帖子（parent_id IS NULL），按创建时间倒序
     */
    @Select("SELECT * FROM post WHERE parent_id IS NULL ORDER BY create_time DESC")
    List<Post> selectTopPosts();

    /**
     * 根据父帖子 ID 查询回复列表
     * @param parentId 父帖子 ID
     */
    @Select("SELECT * FROM post WHERE parent_id = #{parentId} ORDER BY create_time ASC")
    List<Post> selectRepliesByParentId(Long parentId);

    /**
     * 根据 ID 查询单个帖子（包括主帖和回复，用于验证存在性和获取id）
     */
    @Select("SELECT * FROM post WHERE id = #{id}")
    Post selectById(Long id);

    /**
     * 根据 ID 和删除密钥删除帖子（验证 delete_key）
     * @param id        帖子 ID
     * @param deleteKey 删除密钥
     * @return 删除的行数（0 或 1）
     */
    @Delete("DELETE FROM post WHERE id = #{id} AND delete_key = #{deleteKey}")
    int deleteByIdAndKey(@Param("id") Long id, @Param("deleteKey") String deleteKey);

    @Delete("DELETE FROM post WHERE id = #{id}")
    int deleteById(Long id);

    @Delete("DELETE FROM post WHERE parent_id = #{parentId}")
    int deleteRepliesByParentId(Long parentId);

    /**
     * 根据 ID 和删除密钥更新帖子内容
     * @param id        帖子 ID
     * @param deleteKey 删除密钥
     * @param title     新标题
     * @param content   新内容
     * @param imagePath 新图片路径
     * @param updateTime 更新时间
     * @return 更新的行数（0 或 1）
     */
    @Update("UPDATE post SET title = #{title}, content = #{content}, image_path = #{imagePath}, update_time = #{updateTime} WHERE id = #{id} AND delete_key = #{deleteKey}")
    int updateByIdAndKey(@Param("id") Long id, @Param("deleteKey") String deleteKey,
                         @Param("title") String title, @Param("content") String content,
                         @Param("imagePath") String imagePath, @Param("updateTime") LocalDateTime updateTime);
}
