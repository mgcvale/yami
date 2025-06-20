package com.yamiapp.repo;

import com.yamiapp.model.Food;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    @Query("select avg(fr.rating) from FoodReview fr where fr.food.id = :id")
    double getAverageRating(Long id);

    @Query("SELECT f FROM Food f WHERE f.restaurant.id = :id")
    List<Food> getRestaurantFoods(@Param("id") Long id);

    @Modifying(clearAutomatically = true)
    @Query(value = """
    UPDATE foods
    SET avg_rating = (
        SELECT AVG(rating)
        FROM food_reviews
        WHERE food_reviews.food_id = foods.food_id
    )
    WHERE foods.food_id = :foodId
    """, nativeQuery = true)
    void updateAverageRating(@Param("foodId") Long foodId);
}