package com.yamiapp.model.dto;

import com.yamiapp.model.Food;
import lombok.Getter;
import lombok.Setter;

import java.util.Objects;

@Getter
@Setter
public class FoodResponseDTO {

    public record EmbeddedReview(String review, Integer rating) {}

    private Long id;
    private String name;
    private String description;
    private Integer restaurantId;
    private String restaurantName;
    private String restaurantShortName;
    private double avgRating;

    private EmbeddedReview review;

    public FoodResponseDTO(Long id, String name, String description, Integer restaurantId, String restaurantName, String restaurantShortName, Double avgRating) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantShortName = restaurantShortName;
        this.avgRating = Objects.requireNonNullElse(avgRating, 0D);
        this.review = null; // no review unless the user requests the food authenticated
    }

    public FoodResponseDTO(Food f) {
        this(f.getId(), f.getName(), f.getDescription(), Math.toIntExact(f.getRestaurant().getId()), f.getRestaurant().getName(), f.getRestaurant().getShortName(), f.getAvgRating());
    }
}
