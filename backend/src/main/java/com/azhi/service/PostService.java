package com.azhi.service;

import com.azhi.pojo.Post;

import java.util.List;

public interface PostService {
    /**
     * 发布新帖或回复
     * @param post 帖子对象（不含 id，parentId 为 null 表示主帖）
     * @return 保存后的帖子（包含生成的 id）
     */
    Post createPost(Post post);

    /**
     * 查询所有帖子（顶级帖子及其嵌套回复）
     * @return 顶级帖子列表，每个帖子包含其回复数组
     */
    List<Post> getAllPosts();

    /**
     * 根据 ID 删除帖子（主帖会级联删除其所有回复）
     * @param id        帖子 ID
     * @param deleteKey 删除密钥
     * @throws RuntimeException 如果密钥错误或帖子不存在
     */
    void deletePost(Long id, String deleteKey);

    void deletePostAsAdmin(Long id);

    /**
     * 更新帖子内容（需要验证删除密钥）
     * @param id        帖子 ID
     * @param deleteKey 删除密钥
     * @param title     新标题（可为 null，表示不更新）
     * @param content   新内容
     * @param imagePath 新图片路径（可为 null，表示不更新）
     * @return 更新后的帖子
     * @throws RuntimeException 如果密钥错误或帖子不存在
     */
    Post updatePost(Long id, String deleteKey, String title, String content, String imagePath);
}
