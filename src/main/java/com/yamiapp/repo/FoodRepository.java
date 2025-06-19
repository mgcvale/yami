package com.yamiapp.repo;

import com.yamiapp.model.Food;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FoodRepository extends JpaRepository<Food, Integer> {
    @Query("select avg(fr.rating) from FoodReview fr where fr.food.id = :id")
    public double getAverageRating(Long id);

    @Query("SELECT f FROM Food f WHERE f.restaurant.id = :id")
    List<Food> getRestaurantFoods(@Param("id") Long id);
}