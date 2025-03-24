package com.yamiapp.model.dto;

import com.yamiapp.model.Food;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FoodResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Integer restaurantId;
    private String restaurantName;

    public FoodResponseDTO(Long id, String name, String description, Integer restaurantId, String restaurantName) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
    }

    public FoodResponseDTO(Food f) {
        this(f.getId(), f.getName(), f.getDescription(), Math.toIntExact(f.getRestaurant().getId()), f.getRestaurant().getName());
    }
}
