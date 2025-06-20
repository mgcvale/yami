package com.yamiapp.model.dto;

import com.yamiapp.model.Food;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class FoodResponseDTO {

    private Long id;
    private String name;
    private String description;
    private Integer restaurantId;
    private String restaurantName;
    private double avgRating;

    public FoodResponseDTO(Long id, String name, String description, Integer restaurantId, String restaurantName, Double avgRating) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.avgRating = Objects.requireNonNullElse(avgRating, 0D);
    }

    public FoodResponseDTO(Food f) {
        this(f.getId(), f.getName(), f.getDescription(), Math.toIntExact(f.getRestaurant().getId()), f.getRestaurant().getName(), f.getAvgRating());
    }
}
