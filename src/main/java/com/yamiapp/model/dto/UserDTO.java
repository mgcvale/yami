package com.yamiapp.model.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private String username;
    private String password;
    private String bio;
    private String location;
    private String email;

    public UserDTO(String username, String password, String bio, String location, String email) {
        this.username = username;
        this.password = password;
        this.bio = bio;
        this.location = location;
        this.email = email;
    }

    public UserDTO copy() {
        return new UserDTO(
                this.username,
                this.password,
                this.bio,
                this.location,
                this.email
        );
    }

    public UserDTO() {}

    public UserDTO withUsername(String username) {
        this.username = username;
        return this;
    }

    public UserDTO withPassword(String password) {
        this.password = password;
        return this;
    }

    public UserDTO withBio(String bio) {
        this.bio = bio;
        return this;
    }

    public UserDTO withLocation(String location) {
        this.location = location;
        return this;
    }

    public UserDTO  withEmail(String email) {
        this.email = email;
        return this;
    }
}
