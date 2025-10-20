package com.yamiapp.model.dto;

import com.yamiapp.model.FoodReview;
import lombok.Data;

@Data
public class FoodReviewResponseDTO {
    private Long id;

    private String review;

    private Integer rating;

    private Long userId;
    private String username;

    private Long foodId;
    private String foodName;

    private Long restaurantId;
    private String restaurantName;
    private String restaurantShortName;

    public FoodReviewResponseDTO(FoodReview foodReview) {
        this.id = foodReview.getId();
        this.review = foodReview.getReview();
        this.rating = foodReview.getRating();
        this.userId = foodReview.getUser().getId();
        this.username = foodReview.getUser().getUsername();
        this.foodId = foodReview.getFood().getId();
        this.foodName = foodReview.getFood().getName();
        this.restaurantId = foodReview.getFood().getRestaurant().getId();
        this.restaurantName = foodReview.getFood().getRestaurant().getName();
        this.restaurantShortName = foodReview.getFood().getRestaurant().getShortName();
    }

    public FoodReviewResponseDTO(
        Long id, String review, Integer rating,
        Long userId, String username,
        Long foodId, String foodName,
        Long restaurantId, String restaurantName, String restaurantShortName
    ) {
        this.id = id;
        this.review = review;
        this.rating = rating;
        this.userId = userId;
        this.username = username;
        this.foodId = foodId;
        this.foodName = foodName;
        this.restaurantId = restaurantId;
        this.restaurantName = restaurantName;
        this.restaurantShortName = restaurantShortName;
    }
}
