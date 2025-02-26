package com.food.project.model.dto;

import com.food.project.model.User;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDTO {
    private long id;
    private String username;
    private String bio;
    private String location;

    public UserResponseDTO(User u) {
        this.id = u.getId();
        this.username = u.getUsername();
        this.bio = u.getBio();
        this.location = u.getLocation();
    }
}
