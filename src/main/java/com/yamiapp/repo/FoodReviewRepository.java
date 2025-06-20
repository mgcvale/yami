package com.yamiapp.repo;


import com.yamiapp.model.FoodReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

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
}
