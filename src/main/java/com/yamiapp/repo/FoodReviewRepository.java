package com.yamiapp.repo;


import com.yamiapp.model.FoodReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface FoodReviewRepository extends JpaRepository<FoodReview, Long> {

    @Query("select fr from FoodReview fr join fetch fr.food join fetch fr.user where fr.user.id = :userId")
    public Page<FoodReview> getFoodReviewsByUserId(Long userId, Pageable pageable);

    @Query("select fr from FoodReview fr join fetch fr.food join fetch fr.user where fr.food.id = :foodId")
    public Page<FoodReview> getFoodReviewsByFoodId(Long foodId, Pageable pageable);

    Page<FoodReview> findAll(Specification<FoodReview> spec, Pageable pageable);
}
