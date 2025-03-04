package com.yamiapp.model.dto;

import com.yamiapp.model.Food;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodResponseDTO {

    private String name;
    private String description;
    private Integer restaurantId;

    public FoodResponseDTO(String name, String description, Integer restaurantId) {
        this.name = name;
        this.description = description;
        this.restaurantId = restaurantId;
    }

    public FoodResponseDTO(Food f) {
        this(f.getName(), f.getDescription(), Math.toIntExact(f.getRestaurant().getId()));
    }
}
