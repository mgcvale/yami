package com.yamiapp.model.dto;

import com.yamiapp.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDTO {
    private Long id;
    private String username;
    private String bio;
    private String location;
    private String email;
    private String accessToken;
    private Long reviewCount;
    private Long followerCount;
    private Long followingCount;
    private boolean following;

    public UserResponseDTO(User user) {
        this.id = user.getId();
        this.username = user.getUsername();
        this.bio = user.getBio();
        this.location = user.getLocation();
        this.email = user.getEmail();
        this.following = false;
    }

    public UserResponseDTO withCounts(UserCountsDTO userCounts) {
        this.reviewCount = userCounts.getReviewCount();
        this.followerCount = userCounts.getFollowerCount();
        this.followingCount = userCounts.getFollowingCount();
        return this;
    }

    public UserResponseDTO withToken(String token) {
        this.accessToken = token;
        return this;
    }

    public UserResponseDTO withoutSensitiveData() {
        this.email = null;
        this.accessToken = null;
        return this;
    }

    public UserResponseDTO withFollowing(boolean following) {
        this.following = following;
        return this;
    }
}