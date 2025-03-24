package com.yamiapp.model.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class FoodReviewDTO {
    private String review;
    private Integer rating;

    public FoodReviewDTO withReview(String review) {
        this.review = review;
        return this;
    }

    public FoodReviewDTO withRating(Integer rating) {
        this.rating = rating;
        return this;
    }
}
