package com.azhi.service.impl;

import com.azhi.mapper.PostMapper;
import com.azhi.pojo.Post;
import com.azhi.service.PostService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PostServiceImpl implements PostService {

    private final PostMapper postMapper;

    @Override
    @Transactional
    public Post createPost(Post post) {
        if (!StringUtils.hasText(post.getContent())) {
            throw new IllegalArgumentException("Content cannot be empty");
        }
        if (!StringUtils.hasText(post.getUsername())) {
            post.setUsername("Anonymous");
        }
        if (!StringUtils.hasText(post.getDeleteKey())) {
            throw new IllegalArgumentException("Delete key cannot be empty");
        }

        LocalDateTime now = LocalDateTime.now();
        post.setCreateTime(now);
        post.setUpdateTime(now);

        if (post.getParentId() != null && postMapper.selectById(post.getParentId()) == null) {
            throw new IllegalArgumentException("Parent post does not exist");
        }

        postMapper.insert(post);
        post.setReplyCount(0);
        return post;
    }

    @Override
    public List<Post> getAllPosts() {
        List<Post> topPosts = postMapper.selectTopPosts();
        for (Post post : topPosts) {
            List<Post> replies = postMapper.selectRepliesByParentId(post.getId());
            post.setReplies(replies);
            post.setReplyCount(replies.size());
        }
        return topPosts;
    }

    @Override
    @Transactional
    public void deletePost(Long id, String deleteKey) {
        if (!StringUtils.hasText(deleteKey)) {
            throw new IllegalArgumentException("Delete key cannot be empty");
        }

        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("Post does not exist");
        }
        if (!deleteKey.equals(post.getDeleteKey())) {
            throw new IllegalArgumentException("Delete key is incorrect");
        }

        if (post.getParentId() == null) {
            postMapper.deleteRepliesByParentId(id);
        }

        int deleted = postMapper.deleteByIdAndKey(id, deleteKey);
        if (deleted == 0) {
            throw new IllegalArgumentException("Delete failed");
        }
    }

    @Override
    @Transactional
    public Post updatePost(Long id, String deleteKey, String title, String content, String imagePath) {
        if (!StringUtils.hasText(deleteKey)) {
            throw new IllegalArgumentException("Delete key cannot be empty");
        }
        if (!StringUtils.hasText(content)) {
            throw new IllegalArgumentException("Content cannot be empty");
        }

        Post post = postMapper.selectById(id);
        if (post == null) {
            throw new IllegalArgumentException("Post does not exist");
        }
        if (!deleteKey.equals(post.getDeleteKey())) {
            throw new IllegalArgumentException("Delete key is incorrect");
        }

        LocalDateTime now = LocalDateTime.now();
        String newTitle = StringUtils.hasText(title) ? title : post.getTitle();
        String newImagePath = imagePath != null ? imagePath : post.getImagePath();

        int updated = postMapper.updateByIdAndKey(id, deleteKey, newTitle, content, newImagePath, now);
        if (updated == 0) {
            throw new IllegalArgumentException("Update failed");
        }

        post.setTitle(newTitle);
        post.setContent(content);
        post.setImagePath(newImagePath);
        post.setUpdateTime(now);
        return post;
    }
}
