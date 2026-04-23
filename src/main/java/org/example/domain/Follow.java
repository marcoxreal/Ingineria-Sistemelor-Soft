package org.example.domain;

import java.time.LocalDateTime;

public class Follow extends Entity<Void> {

    private Integer followerId;
    private Integer followedId;
    private LocalDateTime createdAt;

    public Follow(Integer followerId, Integer followedId, LocalDateTime createdAt) {
        super(null);
        this.followerId = followerId;
        this.followedId = followedId;
        this.createdAt = createdAt;
    }

    public Integer getFollowerId() {
        return followerId;
    }

    public void setFollowerId(Integer followerId) {
        this.followerId = followerId;
    }

    public Integer getFollowedId() {
        return followedId;
    }

    public void setFollowedId(Integer followedId) {
        this.followedId = followedId;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}