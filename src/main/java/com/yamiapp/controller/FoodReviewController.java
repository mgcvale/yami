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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
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

        FoodReviewResponseDTO r = foodReviewService.createFoodReview(dto, token, id);
        return ResponseEntity.ok().body(r);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteFoodReview(
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

    @GetMapping("/from_restaurant/{id}")
    public ResponseEntity<Object> getFoodReviewsFromRestaurant(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") Integer offset,
        @RequestParam(defaultValue = "50") Integer count
    ) {
        return ResponseEntity.ok().body(foodReviewService.getFoodReviewByRestaurant(id, Pageable.ofSize(count).withPage(offset)));
    }

    @GetMapping("/from_user/{id}")
    public ResponseEntity<Object> getUserReviews(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") Integer offset,
        @RequestParam(defaultValue = "50") Integer count,
        @RequestParam(required = false, defaultValue = "") String keyword
    ) {
        Page<FoodReview> foodReviews = foodReviewService.getFoodReviewsByUser(id, "", keyword, Pageable.ofSize(count).withPage(offset));
        Page<FoodReviewResponseDTO> responseReviews = foodReviews.map(FoodReviewResponseDTO::new);
        return ResponseEntity.ok().body(responseReviews);
    }

}
