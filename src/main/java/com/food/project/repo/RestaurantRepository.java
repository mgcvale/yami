package com.food.project.repo;

import com.food.project.model.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;

public abstract class RestaurantRepository implements JpaRepository<Restaurant, Integer> {
}