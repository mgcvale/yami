package com.food.project.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.food.project.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private String username;
    private String password;
    private String bio;
    private String location;

    public UserDTO(String username, String password, String bio, String location) {
        this.username = username;
        this.password = password;
        this.bio = bio;
        this.location = location;
    }
}
