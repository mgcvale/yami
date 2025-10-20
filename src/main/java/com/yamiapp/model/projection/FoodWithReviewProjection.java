package com.yamiapp.model.projection;

public record FoodWithReviewProjection(
    Long foodId,
    String foodName,
    String foodDescription,
    Long restaurantId,
    String restaurantName,
    String restaurantShortName,
    Double avgRating,
    String userReview,
    Integer userRating,
    Long reviewId
) {}