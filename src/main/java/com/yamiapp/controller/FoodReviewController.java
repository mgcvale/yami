package com.yamiapp.controller;

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

import java.net.URI;
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
    public ResponseEntity<FoodReviewResponseDTO> createFoodReview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody FoodReviewDTO dto,
            @PathVariable Long id
    ) {
        String token = ControllerUtils.extractToken(authHeader);
        FoodReviewResponseDTO r = foodReviewService.createFoodReview(dto, token, id);
        return ResponseEntity.created(URI.create("/food/review/" + r.getId())).body(r);
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
    public ResponseEntity<FoodReviewResponseDTO> updateFoodReview(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestBody FoodReviewDTO dto,
            @PathVariable Long id
    ) {
        String token = ControllerUtils.extractToken(authHeader);

        return ResponseEntity.ok(foodReviewService.updateFoodReview(id, dto, token));
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodReviewResponseDTO> getFoodReviewById(
            @PathVariable Long id,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        Optional<String> token = ControllerUtils.extractOptionalToken(authHeader);
        return ResponseEntity.ok(foodReviewService.getFoodReviewById(id, token));
    }

    @GetMapping("/from_restaurant/{id}")
    public ResponseEntity<Page<FoodReviewResponseDTO>> getFoodReviewsFromRestaurant(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") Integer offset,
        @RequestParam(defaultValue = "50") Integer count,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        Optional<String> token = ControllerUtils.extractOptionalToken(authHeader);
        return ResponseEntity.ok().body(foodReviewService.getFoodReviewByRestaurant(id, Pageable.ofSize(count).withPage(offset), token));
    }

    @GetMapping("/from_user/{id}")
    public ResponseEntity<Page<FoodReviewResponseDTO>> getUserReviews(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") Integer offset,
        @RequestParam(defaultValue = "50") Integer count,
        @RequestParam(required = false, defaultValue = "") String keyword,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        Optional<String> token = ControllerUtils.extractOptionalToken(authHeader);
        Page<FoodReviewResponseDTO> foodReviews = foodReviewService.getFoodReviewsByUser(id, "", keyword, Pageable.ofSize(count).withPage(offset), token);
        return ResponseEntity.ok().body(foodReviews);
    }

}
