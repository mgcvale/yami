package com.yamiapp.controller;

import com.yamiapp.model.dto.IsReviewLikedResponse;
import com.yamiapp.model.dto.UserResponseDTO;
import com.yamiapp.service.FoodReviewLikeService;
import com.yamiapp.util.ControllerUtils;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/food/review/")
public class FoodReviewLikeController {
    private final FoodReviewLikeService foodReviewLikeService;

    public FoodReviewLikeController(FoodReviewLikeService foodReviewLikeService) {
        this.foodReviewLikeService = foodReviewLikeService;
    }

    @PostMapping("/{id}/like")
    public ResponseEntity<Void> likeFoodReview(
        @PathVariable Long id,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = ControllerUtils.extractToken(authHeader);
        foodReviewLikeService.likeReview(token, id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/like")
    public ResponseEntity<Void> unlikeFoodReview(
        @PathVariable Long id,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        String token = ControllerUtils.extractToken(authHeader);
        foodReviewLikeService.unlikeReview(token, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/like/{userId}")
    public ResponseEntity<IsReviewLikedResponse> isFoodReviewLiked(
        @PathVariable Long id,
        @PathVariable Long userId
    ) {
        boolean liked = foodReviewLikeService.isReviewLikedByUser(userId, id);
        return ResponseEntity.ok(new IsReviewLikedResponse(liked));
    }

    @GetMapping("/{id}/like")
    public ResponseEntity<Page<UserResponseDTO>> getLikersFromReview(
        @PathVariable Long id,
        @RequestParam(defaultValue = "0") Integer offset,
        @RequestParam(defaultValue = "50") Integer count
    ) {
        return ResponseEntity.ok(foodReviewLikeService.getLikersFromFoodReview(id, Pageable.ofSize(count).withPage(offset)));
    }

}
