package com.yamiapp.service;

import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.NotFoundException;
import com.yamiapp.model.FoodReview;
import com.yamiapp.model.ReviewLike;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserResponseDTO;
import com.yamiapp.model.projection.ReviewLikeCountProjection;
import com.yamiapp.model.projection.ReviewLikedProjection;
import com.yamiapp.repo.FoodReviewRepository;
import com.yamiapp.repo.ReviewLikeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class FoodReviewLikeService {

    private ReviewLikeRepository reviewLikeRepository;
    private UserService userService;
    private FoodReviewRepository frRepository;

    public FoodReviewLikeService(ReviewLikeRepository reviewLikeRepository, UserService userService, FoodReviewRepository frRepository) {
        this.reviewLikeRepository = reviewLikeRepository;
        this.userService = userService;
        this.frRepository = frRepository;
    }

    public ReviewLike likeReview(String accessToken, Long frId) {
        User user = userService.getRawByToken(accessToken);
        FoodReview foodReview = frRepository.findById(frId).orElseThrow(() -> new NotFoundException(ErrorStrings.INVALID_FOOD_REVIEW_ID.getMessage()));

        ReviewLike rl = new ReviewLike();
        rl.setUser(user);
        rl.setReview(foodReview);

        return reviewLikeRepository.save(rl);
    }

    public void unlikeReview(String accessToken, Long frId) {
        User user = userService.getRawByToken(accessToken);
        FoodReview fr = frRepository.findById(frId).orElseThrow(() -> new NotFoundException(ErrorStrings.INVALID_FOOD_REVIEW_ID.getMessage()));

        reviewLikeRepository.deleteReviewLikeByAccessTokenAndReviewId(accessToken, frId);
    }

    public Page<FoodReview> getLikedFoodReviewsByUser(String accessToken, Pageable pageable) {
        return reviewLikeRepository.getLikedReviewsByUser(accessToken, pageable);
    }

    public Page<User> getRawLikersFromFoodReview(Long foodReviewId, Pageable pageable) {
        return reviewLikeRepository.getReviewLikes(foodReviewId, pageable);
    }

    public Page<UserResponseDTO> getLikersFromFoodReview(Long foodReviewId, Pageable pageable) {
        return getRawLikersFromFoodReview(foodReviewId, pageable).map(u -> new UserResponseDTO(u).withoutSensitiveData());
    }

    public boolean isReviewLikedByUser(Long userId, Long foodReviewId) {
        return reviewLikeRepository.existsByUserIdAndReviewId(userId, foodReviewId);
    }

    public boolean isReviewLikedByUser(String accessToken, Long foodReviewId) {
        return reviewLikeRepository.existsByUserAccessTokenAndReviewId(accessToken, foodReviewId);
    }

    public Long countFoodReviewLikes(Long foodReviewId) {
        return reviewLikeRepository.countByReviewId(foodReviewId);
    }

    public List<ReviewLikeCountProjection> fillFoodReviewLikeCounts(List<Long> foodReviewIds) {
        return reviewLikeRepository.countLikesByReviewIds(foodReviewIds);
    }

    public List<ReviewLikedProjection> fillFoodReviewLiked(List<Long> foodReviewIds, String accessToken) {
        return reviewLikeRepository.batchedExistsByUserAccessTokenAndReviewId(foodReviewIds, accessToken);
    }
}
