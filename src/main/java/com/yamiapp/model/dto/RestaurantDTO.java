package com.yamiapp.model.dto;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
public class RestaurantDTO {

    private String name;
    private String shortName;
    private MultipartFile photo;
    private String description;

    public RestaurantDTO(String name, String shortName, MultipartFile photo, String description) {
        this.name = name;
        this.photo = photo;
        this.description = description;
        this.shortName = shortName;
    }

}
