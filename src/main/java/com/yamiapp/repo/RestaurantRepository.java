package com.yamiapp.repo;

import com.yamiapp.model.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {
    @Query("select r from Restaurant r where r.name ilike %:searchParams%")
    Page<Restaurant> getRestaurantsByAnonymousSearch(@Param("searchParams") String searchParams, Pageable pageable);
}