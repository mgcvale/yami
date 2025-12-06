package com.yamiapp.repo;

import com.yamiapp.model.Food;
import com.yamiapp.model.FoodReview;
import com.yamiapp.model.dto.RatingDistributionEntry;
import com.yamiapp.model.projection.FoodWithReviewProjection;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FoodRepository extends JpaRepository<Food, Long> {
    @Query("select avg(fr.rating) from FoodReview fr where fr.food.id = :id")
    double getAverageRating(Long id);

    @EntityGraph(attributePaths = {"food", "food.restaurant"})
    @Query("SELECT f FROM Food f WHERE f.restaurant.id = :id")
    List<Food> getRestaurantFoods(@Param("id") Long id);

    @EntityGraph(attributePaths = {"food", "food.restaurant"})
    @Query("SELECT f from Food f where f.restaurant.id = :id and f.name ilike %:query%")
    List<Food> searchRestaurantFoods(@Param("id") Long id, @Param("query") String query);

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

    @Query("""
    SELECT new com.yamiapp.model.projection.FoodWithReviewProjection(
        f.id, f.name, f.description, r.id, r.name, r.shortName, f.avgRating,
        fr.review, fr.rating, fr.id
    )
    FROM Food f
    LEFT JOIN f.restaurant r
    LEFT JOIN FoodReview fr ON f.id = fr.food.id AND fr.user.accessToken = :accessToken
    WHERE f.id = :foodId
    """)
    Optional<FoodWithReviewProjection> findFoodByIdWithUserReviewProjection(@Param("foodId") Long foodId, @Param("accessToken") String accessToken);

    @Query(value = """
        select fr.rating as k, COUNT(*) as v
        from food_reviews fr
        where fr.food_id = :foodId
        group by fr.rating
    """, nativeQuery = true)
    List<RatingDistributionEntry> getRatingDistribution(@Param("foodId") Long foodId);

    @Query("select f from Food f join fetch f.restaurant where f.id = :id")
    Optional<Food> findByIdWithRestaurant(@Param("id") Long id);

}