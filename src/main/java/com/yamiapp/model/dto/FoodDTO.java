package com.yamiapp.model.dto;


import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@Builder
public class FoodDTO {
    private String name;
    private String description;
    private MultipartFile photo;
    private Integer restaurantId;
}
