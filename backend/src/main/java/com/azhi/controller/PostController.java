package com.azhi.controller;

import com.azhi.pojo.Post;
import com.azhi.pojo.Result;
import com.azhi.service.PostService;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/posts")
@RequiredArgsConstructor
public class PostController {

    private final PostService postService;

    @PostMapping
    public Result<Post> createPost(HttpSession session, @RequestBody Post post) {
        requireLogin(session);
        return Result.success(postService.createPost(post));
    }

    @GetMapping
    public Result<List<Post>> getPosts() {
        return Result.success(postService.getAllPosts());
    }

    @DeleteMapping("/{postId}")
    public Result<Void> deletePost(@PathVariable Long postId, @RequestBody(required = false) Map<String, String> body) {
        String deleteKey = body == null ? null : body.get("deleteKey");
        postService.deletePost(postId, deleteKey);
        return Result.success();
    }

    @PutMapping("/{postId}")
    public Result<Post> updatePost(@PathVariable Long postId, @RequestBody Map<String, String> body) {
        String deleteKey = body.get("deleteKey");
        String title = body.get("title");
        String content = body.get("content");
        String imagePath = body.get("imagePath");
        return Result.success(postService.updatePost(postId, deleteKey, title, content, imagePath));
    }

    private void requireLogin(HttpSession session) {
        if (session.getAttribute("currentUser") == null) {
            throw new IllegalArgumentException("请先登录");
        }
    }
}
