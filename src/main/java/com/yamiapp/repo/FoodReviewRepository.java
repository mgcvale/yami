package com.yamiapp.repo;


import com.yamiapp.model.FoodReview;
import com.yamiapp.model.dto.FoodReviewResponseDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FoodReviewRepository extends JpaRepository<FoodReview, Long> {

    @EntityGraph(attributePaths = {"food", "food.restaurant", "user"})
    @Query("select fr from FoodReview fr where fr.user.id = :userId")
    Page<FoodReview> getFoodReviewsByUserId(@Param("userId") Long userId, Pageable pageable);

    @EntityGraph(attributePaths = {"food", "food.restaurant", "user"})
    @Query("select fr from FoodReview fr where fr.food.id = :foodId")
    Page<FoodReview> getFoodReviewsByFoodId(@Param("foodId") Long foodId, Pageable pageable);

    @EntityGraph(attributePaths = {"food", "food.restaurant"})
    @Query("select fr from FoodReview fr where fr.food.restaurant.id = :restaurantId")
    Page<FoodReview> getFoodReviewsByRestaurantId(@Param("restaurantId") Long restaurantId, Pageable pageable);

    Page<FoodReview> findAll(Specification<FoodReview> spec, Pageable pageable);

    @EntityGraph(attributePaths = {"user", "food", "food.restaurant"})
    @Query("""
        SELECT new com.yamiapp.model.dto.FoodReviewResponseDTO(
            fr.id,
            fr.review,
            fr.rating,
            fr.user.id,
            fr.user.username,
            fr.food.id,
            fr.food.name,
            fr.food.restaurant.id,
            fr.food.restaurant.name,
            fr.food.restaurant.shortName
        )
        FROM FoodReview fr
        WHERE fr.user.id IN (
            SELECT f.id
            FROM User me
            JOIN me.following f
            WHERE me.accessToken = :accessToken
        )
        ORDER BY fr.createdAt DESC
    """)
    Page<FoodReviewResponseDTO> findUserFeed(@Param("accessToken") String userToken, Pageable pageable);

}
