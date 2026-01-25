package com.chefmate.backend.dto;

import java.time.LocalDateTime;
import java.util.List;

public class CommentResponse {
    private Long id;
    private String content;
    private Long userId;
    private String username;
    private LocalDateTime createdAt;
    private Long parentCommentId;
    private List<CommentResponse> replies;
    private Integer likesCount;
    private Boolean isLiked;
    private String userProfileImageUrl;

    public CommentResponse() {}

    public CommentResponse(Long id, String content, Long userId, String username, LocalDateTime createdAt) {
        this.id = id;
        this.content = content;
        this.userId = userId;
        this.username = username;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public Long getParentCommentId() { return parentCommentId; }
    public void setParentCommentId(Long parentCommentId) { this.parentCommentId = parentCommentId; }

    public List<CommentResponse> getReplies() { return replies; }
    public void setReplies(List<CommentResponse> replies) { this.replies = replies; }

    public Integer getLikesCount() { return likesCount; }
    public void setLikesCount(Integer likesCount) { this.likesCount = likesCount; }

    public Boolean getIsLiked() { return isLiked; }
    public void setIsLiked(Boolean isLiked) { this.isLiked = isLiked; }

    public String getUserProfileImageUrl() { return userProfileImageUrl; }
    public void setUserProfileImageUrl(String userProfileImageUrl) { this.userProfileImageUrl = userProfileImageUrl; }
}


