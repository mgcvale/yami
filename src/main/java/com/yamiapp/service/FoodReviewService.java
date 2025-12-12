package com.yamiapp.service;

import com.yamiapp.exception.*;
import com.yamiapp.model.Food;
import com.yamiapp.model.FoodReview;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.FoodReviewDTO;
import com.yamiapp.model.dto.FoodReviewResponseDTO;
import com.yamiapp.model.projection.ReviewLikeCountProjection;
import com.yamiapp.model.projection.ReviewLikedProjection;
import com.yamiapp.repo.FoodReviewRepository;
import com.yamiapp.repo.FoodRepository;
import com.yamiapp.validator.FoodReviewCreateValidator;
import com.yamiapp.validator.FoodReviewUpdateValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class FoodReviewService {

    private final FoodReviewRepository foodReviewRepository;
    private final FoodRepository foodRepository;
    private final UserService userService;
    private final FoodReviewCreateValidator createValidator;
    private final FoodReviewUpdateValidator updateValidator;
    private final FoodReviewLikeService foodReviewLikeService;

    public FoodReviewService(FoodReviewRepository foodReviewRepository,
                             FoodRepository foodRepository,
                             UserService userService,
                             FoodReviewCreateValidator createValidator,
                             FoodReviewUpdateValidator updateValidator,
                             FoodReviewLikeService foodReviewLikeService) {
        this.foodReviewRepository = foodReviewRepository;
        this.foodRepository = foodRepository;
        this.userService = userService;
        this.createValidator = createValidator;
        this.updateValidator = updateValidator;
        this.foodReviewLikeService = foodReviewLikeService;
    }

    @Transactional
    public FoodReviewResponseDTO createFoodReview(FoodReviewDTO dto, String token, Long foodId) {
        return new FoodReviewResponseDTO(createRawFoodReview(dto, token, foodId)); // a new food review has no likes, so we don't have to apply like count or liked, as they initialize to 0 and false on the DTO
    }

    @Transactional
    public FoodReview createRawFoodReview(FoodReviewDTO dto, String token, Long foodId) {
        createValidator.validate(dto);
        User user = userService.getRawByToken(token);

        Food food = foodRepository.findByIdWithRestaurant(foodId).orElseThrow(() -> new NotFoundException(ErrorStrings.INVALID_FOOD_ID.getMessage()));

        FoodReview review = new FoodReview();
        review.setReview(dto.getReview());
        review.setRating(dto.getRating());
        review.setUser(user);
        review.setFood(food);
        FoodReview result = foodReviewRepository.save(review);

        // now we update that food's average rating
        foodRepository.updateAverageRating(food.getId());

        return result;
    }

    @Transactional
    public FoodReview updateRawFoodReview(Long reviewId, FoodReviewDTO dto, String token) {
        updateValidator.validate(dto);
        User user = userService.getRawByToken(token);

        Optional<FoodReview> optReview = foodReviewRepository.findById(reviewId);
        if (optReview.isEmpty()) {
            throw new NotFoundException(ErrorStrings.INVALID_FOOD_REVIEW_ID.getMessage());
        }
        FoodReview review = optReview.get();

        // Ensure the review belongs to the user making the request
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorStrings.NOT_OWNER_OF_FOOD_REVIEW.getMessage());
        }

        if (dto.getReview() != null) {
            review.setReview(dto.getReview());
        }
        if (dto.getRating() != null) {
            review.setRating(dto.getRating());
            // we only update the food avg rating if the rating was altered
            foodRepository.updateAverageRating(review.getFood().getId());
        }

        return foodReviewRepository.save(review);
    }

    @Transactional
    public FoodReviewResponseDTO updateFoodReview(Long reviewId, FoodReviewDTO dto, String token) {
        FoodReview updated = updateRawFoodReview(reviewId, dto, token);
        FoodReviewResponseDTO res = new FoodReviewResponseDTO(updated);
        res.setLikeCount(foodReviewLikeService.countFoodReviewLikes(reviewId));
        applyLiked(res, token);
        return res;
    }

    @Transactional
    public void deleteFoodReview(Long reviewId, String token) {
        User user = userService.getRawByToken(token);

        Optional<FoodReview> optReview = foodReviewRepository.findById(reviewId);
        if (optReview.isEmpty()) {
            throw new NotFoundException(ErrorStrings.INVALID_FOOD_REVIEW_ID.getMessage());
        }
        FoodReview review = optReview.get();
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorStrings.NOT_OWNER_OF_FOOD_REVIEW.getMessage());
        }

        Long foodId = review.getFood().getId();
        foodReviewRepository.delete(review);

        foodRepository.updateAverageRating(foodId);
    }

    public Page<FoodReview> getRawFoodReviewsByFoodId(Long foodId, String reviewKeyword, Pageable pageable) {
        if (!foodRepository.existsById(foodId)) {
            throw new NotFoundException(ErrorStrings.INVALID_FOOD_ID.getMessage());
        }

        if (reviewKeyword == null || reviewKeyword.isBlank()) {
            return foodReviewRepository.getFoodReviewsByFoodId(foodId, pageable);
        } else {

            Specification<FoodReview> spec = (root, query, cb) -> {
                query.distinct(true);

                var predicates = cb.equal(root.get("food").get("id"), foodId);
                predicates = cb.and(predicates,
                        cb.like(cb.lower(root.get("review")), "%" + reviewKeyword.toLowerCase() + "%"));
                return predicates;
            };
            return foodReviewRepository.findAll(spec, pageable);
        }
    }

    public Page<FoodReviewResponseDTO> getFoodReviewsByFoodId(Long foodId, String reviewKeyword, Pageable pageable, Optional<String> accessToken) {
        Page<FoodReviewResponseDTO> response = applyLikeCountsAndDTO(getRawFoodReviewsByFoodId(foodId, reviewKeyword, pageable));
        return accessToken.map(s -> applyLikedBatched(response, s)).orElse(response);
    }


    public Page<FoodReview> getRawFoodReviewsByUser(Long userId, String foodName, String reviewKeyword, Pageable pageable) {
        if (!userService.userExists(userId)) {
            throw new NotFoundException(ErrorStrings.INVALID_USER_ID.getMessage());
        }

        if ((foodName == null || foodName.isBlank()) && (reviewKeyword == null || reviewKeyword.isBlank())) {
            return foodReviewRepository.getFoodReviewsByUserId(userId, pageable);
        } else {
            Specification<FoodReview> spec = (root, query, cb) -> {
                var predicates = cb.conjunction();
                // filter by user id
                predicates = cb.and(predicates, cb.equal(root.get("user").get("id"), userId));
                if (foodName != null && !foodName.isBlank()) {
                    predicates = cb.and(predicates,
                            cb.like(cb.lower(root.get("food").get("name")), "%" + foodName.toLowerCase() + "%"));
                }
                if (reviewKeyword != null && !reviewKeyword.isBlank()) {
                    predicates = cb.and(predicates,
                            cb.like(cb.lower(root.get("review")), "%" + reviewKeyword.toLowerCase() + "%"));
                }
                return predicates;
            };
            return foodReviewRepository.findAll(spec, pageable);
        }
    }

    public Page<FoodReviewResponseDTO> getFoodReviewsByUser(Long userId, String foodName, String reviewKeyword, Pageable pageable, Optional<String> accessToken) {
        Page<FoodReviewResponseDTO> reviews = applyLikeCountsAndDTO(getRawFoodReviewsByUser(userId, foodName, reviewKeyword, pageable));
        return accessToken.map(s -> applyLikedBatched(reviews, s)).orElse(reviews);
    }

    public Page<FoodReviewResponseDTO> getFoodReviewsByFollowers(String authToken, Pageable pageable) {
        Page<FoodReviewResponseDTO> foodReviews = applyLikeCounts(foodReviewRepository.findUserFeed(authToken, pageable));
        return applyLikedBatched(foodReviews, authToken);
    }

    public Page<FoodReviewResponseDTO> getFoodReviewByRestaurant(Long restaurantId, Pageable pageable, Optional<String> accessToken) {
        Page<FoodReviewResponseDTO> response = applyLikeCounts(foodReviewRepository.getFoodReviewsByRestaurantId(restaurantId, pageable).map(FoodReviewResponseDTO::new));
        return accessToken.map(s -> applyLikedBatched(response, s)).orElse(response);
    }

    public Optional<FoodReview> getRawFoodReviewById(Long foodReviewId) {
        return foodReviewRepository.findById(foodReviewId);
    }

    public FoodReviewResponseDTO getFoodReviewById(Long foodReviewId, Optional<String> accessToken) {
        FoodReview r = foodReviewRepository.findById(foodReviewId).orElseThrow(() -> new NotFoundException(ErrorStrings.INVALID_FOOD_REVIEW_ID.getMessage()));
        FoodReviewResponseDTO res = new FoodReviewResponseDTO(r);
        res.setLikeCount(foodReviewLikeService.countFoodReviewLikes(res.getId()));
        accessToken.ifPresent(s -> applyLiked(res, s));
        return res;
    }

    private Page<FoodReviewResponseDTO> applyLikeCounts(Page<FoodReviewResponseDTO> response) {
        List<Long> ids = response.stream().map(FoodReviewResponseDTO::getId).toList();
        Map<Long, Long> idsToLikeCount = foodReviewLikeService.fillFoodReviewLikeCounts(ids)
            .stream()
            .collect(Collectors.toMap(
                ReviewLikeCountProjection::reviewId,
                ReviewLikeCountProjection::count
            ));

        return response.map(review -> {
            review.setLikeCount(idsToLikeCount.get(review.getId()));
            return review;
        });
    }

    private Page<FoodReviewResponseDTO> applyLikeCountsAndDTO(Page<FoodReview> response) {
        List<Long> ids = response.stream().map(FoodReview::getId).toList();
        Map<Long, Long> idsToLikeCount = foodReviewLikeService.fillFoodReviewLikeCounts(ids)
            .stream()
            .collect(Collectors.toMap(
                ReviewLikeCountProjection::reviewId,
                ReviewLikeCountProjection::count
            ));

        return response.map(review -> {
            FoodReviewResponseDTO res = new FoodReviewResponseDTO(review);
            res.setLikeCount(idsToLikeCount.getOrDefault(res.getId(), 0L));
            return res;
        });
    }

    private void applyLiked(FoodReviewResponseDTO dto, String accessToken) {
        dto.setLiked(foodReviewLikeService.isReviewLikedByUser(accessToken, dto.getId()));
    }

    private Page<FoodReviewResponseDTO> applyLikedBatched(Page<FoodReviewResponseDTO> dto, String accessToken) {
        List<Long> ids = dto.stream().map(FoodReviewResponseDTO::getId).toList();
        Map<Long, Boolean> idsToLiked = foodReviewLikeService.fillFoodReviewLiked(ids, accessToken)
            .stream()
            .collect(Collectors.toMap(
                ReviewLikedProjection::reviewId,
                ReviewLikedProjection::liked
            ));

        return dto.map(review -> {
            review.setLiked(idsToLiked.getOrDefault(review.getId(), false));
            return review;
        });
    }
}