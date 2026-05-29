package com.azhi.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    private Long id;
    private Long postId;
    private Long parentId;
    private String username;
    private String email;
    private String title;
    private String content;
    private String imagePath;
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String deleteKey;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime;
    private Integer replyCount;
    private List<Post> replies = new ArrayList<>();

    public Long getPostId() {
        return postId != null ? postId : id;
    }

    public void setId(Long id) {
        this.id = id;
        this.postId = id;
    }

    public void setPostId(Long postId) {
        this.postId = postId;
        this.id = postId;
    }
}
