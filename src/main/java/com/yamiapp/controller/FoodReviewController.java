package com.yamiapp.controller;

import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.NotFoundException;
import com.yamiapp.model.FoodReview;
import com.yamiapp.model.dto.FoodReviewDTO;
import com.yamiapp.model.dto.FoodReviewResponseDTO;
import com.yamiapp.service.FoodReviewService;
import com.yamiapp.util.ControllerUtils;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/food/review")
public class FoodReviewController {

    private final FoodReviewService foodReviewService;

    public FoodReviewController(
            final FoodReviewService foodReviewService
    ) {
        this.foodReviewService = foodReviewService;
    }

    @PostMapping("/{id}")
    public ResponseEntity<Object> createFoodReview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody FoodReviewDTO dto,
            @PathVariable Long id
    ) {
        String token = ControllerUtils.extractToken(authHeader);

        FoodReview r = foodReviewService.createFoodReview(dto, token, id);
        return ResponseEntity.ok().body(new FoodReviewResponseDTO(r));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteFoodReview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable Long id
    ) {
        String token = ControllerUtils.extractToken(authHeader);

        foodReviewService.deleteFoodReview(id, token);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOOD_REVIEW_DELETE_SUCCESS.getMessage());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> updateFoodReview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody FoodReviewDTO dto,
            @PathVariable Long id
    ) {
        String token = ControllerUtils.extractToken(authHeader);

        FoodReview f = foodReviewService.updateFoodReview(id, dto, token);
        return ResponseEntity.ok().body(new FoodReviewResponseDTO(f));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getFoodReviewById(
            @PathVariable Long id
    ) {
        Optional<FoodReview> optFoodReview = foodReviewService.getFoodReviewById(id);
        if (optFoodReview.isPresent()) {
            return ResponseEntity.ok().body(new FoodReviewResponseDTO(optFoodReview.get()));
        }
        throw new NotFoundException(ErrorStrings.INVALID_FOOD_ID.getMessage());
    }
}
