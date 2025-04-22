package com.yamiapp.repo;

import com.yamiapp.model.Food;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

@Repository
public interface FoodRepository extends JpaRepository<Food, Integer> {
    @Query("select avg(fr.rating) from FoodReview fr where fr.food.id = :id")
    public double getAverageRating(Long id);
}
