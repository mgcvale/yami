package com.yamiapp.repo;

import com.yamiapp.model.FoodReview;
import com.yamiapp.model.ReviewLike;
import com.yamiapp.model.User;
import com.yamiapp.model.projection.ReviewLikeCountProjection;
import com.yamiapp.model.projection.ReviewLikedProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

public interface ReviewLikeRepository extends JpaRepository<ReviewLike, Long> {

    @Query("select r from FoodReview r join ReviewLike rl on rl.review = r where rl.user.accessToken = :accessToken")
    public Page<FoodReview> getLikedReviewsByUser(@Param("accessToken") String accessToken, Pageable pageable);

    @Query("select u from User u join ReviewLike rl on rl.user = u where rl.review.id = :reviewId")
    public Page<User> getReviewLikes(@Param("reviewId") Long reviewId, Pageable pageable);

    @Transactional
    @Modifying
    @Query("delete ReviewLike rl where rl.user.accessToken = :accessToken and rl.review.id = :reviewId")
    public void deleteReviewLikeByAccessTokenAndReviewId(@Param("accessToken") String accessToken, @Param("reviewId") Long reviewId);

    public boolean existsByUserIdAndReviewId(Long userId, Long reviewId);

    @Query("select (count(r) > 0) from ReviewLike r where r.user.accessToken = :accessToken and r.review.id = :reviewId")
    public boolean existsByUserAccessTokenAndReviewId(@Param("accessToken") String accessToken, @Param("reviewId") Long reviewId);

    @Query("""
        select new com.yamiapp.model.projection.ReviewLikedProjection(rl.review.id, (count(rl) > 0))
        from ReviewLike rl
        where rl.review.id in :reviewIds
        and rl.user.accessToken = :accessToken
        group by rl.review.id
    """)
    List<ReviewLikedProjection> batchedExistsByUserAccessTokenAndReviewId(@Param("reviewIds") List<Long> reviewIds, @Param("accessToken") String accessToken);

    Long countByReviewId(Long foodReviewId);

    @Query("""
        select new com.yamiapp.model.projection.ReviewLikeCountProjection(rl.review.id, count(rl))
        from ReviewLike rl
        where rl.review.id in :reviewIds
        group by rl.review.id
    """)
    List<ReviewLikeCountProjection> countLikesByReviewIds(@Param("reviewIds") List<Long> reviewIds);

}
