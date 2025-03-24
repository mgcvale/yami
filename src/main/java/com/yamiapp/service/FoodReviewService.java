package com.yamiapp.service;

import com.yamiapp.exception.*;
import com.yamiapp.model.Food;
import com.yamiapp.model.FoodReview;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.FoodReviewDTO;
import com.yamiapp.repo.FoodReviewRepository;
import com.yamiapp.repo.FoodRepository;
import com.yamiapp.validator.FoodReviewCreateValidator;
import com.yamiapp.validator.FoodReviewUpdateValidator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class FoodReviewService {

    private final FoodReviewRepository foodReviewRepository;
    private final FoodRepository foodRepository;
    private final UserService userService;
    private final FoodReviewCreateValidator createValidator;
    private final FoodReviewUpdateValidator updateValidator;

    public FoodReviewService(FoodReviewRepository foodReviewRepository,
                             FoodRepository foodRepository,
                             UserService userService,
                             FoodReviewCreateValidator createValidator,
                             FoodReviewUpdateValidator updateValidator) {
        this.foodReviewRepository = foodReviewRepository;
        this.foodRepository = foodRepository;
        this.userService = userService;
        this.createValidator = createValidator;
        this.updateValidator = updateValidator;
    }

    @Transactional
    public FoodReview createFoodReview(FoodReviewDTO dto, String token, Long foodId) {
        createValidator.validate(dto);
        User user = userService.getByToken(token);

        Optional<Food> optFood = foodRepository.findById(Math.toIntExact(foodId));
        if (optFood.isEmpty()) {
            throw new NotFoundException(ErrorStrings.INVALID_FOOD_ID.getMessage());
        }
        Food food = optFood.get();

        FoodReview review = new FoodReview();
        review.setReview(dto.getReview());
        review.setRating(dto.getRating());
        review.setUser(user);
        review.setFood(food);

        return foodReviewRepository.save(review);
    }

    @Transactional
    public FoodReview updateFoodReview(Long reviewId, FoodReviewDTO dto, String token) {
        // Validate update DTO
        updateValidator.validate(dto);
        // Validate token and get current user
        User user = userService.getByToken(token);

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
        }

        return foodReviewRepository.save(review);
    }

    @Transactional
    public void deleteFoodReview(Long reviewId, String token) {
        User user = userService.getByToken(token);
        Optional<FoodReview> optReview = foodReviewRepository.findById(reviewId);
        if (optReview.isEmpty()) {
            throw new NotFoundException(ErrorStrings.INVALID_FOOD_REVIEW_ID.getMessage());
        }
        FoodReview review = optReview.get();
        if (!review.getUser().getId().equals(user.getId())) {
            throw new ForbiddenException(ErrorStrings.NOT_OWNER_OF_FOOD_REVIEW.getMessage());
        }
        foodReviewRepository.delete(review);
    }

    public Page<FoodReview> getFoodReviewsByFoodId(Long foodId, String reviewKeyword, Pageable pageable) {
        if (!foodRepository.existsById(Math.toIntExact(foodId))) {
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


    public Page<FoodReview> getFoodReviewsByUser(Long userId, String foodName, String reviewKeyword, Pageable pageable) {
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

    public Optional<FoodReview> getFoodReviewById(Long foodReviewId) {
        return foodReviewRepository.findById(foodReviewId);
    }
}