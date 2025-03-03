package com.yamiapp.model.dto;

import com.yamiapp.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDTO {
    private long id;
    private String username;
    private String bio;
    private String location;
    private String email;
    private String accessToken;

    public UserResponseDTO(User u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.bio = u.getBio();
        this.location = u.getLocation();
        this.email = u.getEmail();
    }

    public UserResponseDTO withToken(String accessToken) {
        this.accessToken = accessToken;
        return this;
    }

    public UserResponseDTO withoutSensitiveData() {
        this.email = null;
        return this;
    }
}
