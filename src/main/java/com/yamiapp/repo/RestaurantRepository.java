package com.yamiapp.repo;

import com.yamiapp.model.Restaurant;
import com.yamiapp.model.dto.RestaurantResposneDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {

    @Query("""
        SELECT new com.yamiapp.model.dto.RestaurantResposneDTO(
            r.id, r.name, r.shortName, r.description,
            COALESCE(COUNT(DISTINCT f.id), 0),
            COALESCE(COUNT(DISTINCT fr.id), 0)
        )
        FROM Restaurant  r
        LEFT JOIN r.foods f
        LEFT JOIN FoodReview fr on fr.food.id = f.id
        WHERE r.id=:id
        GROUP BY r.id, r.name, r.shortName, r.description
    """)
    Optional<RestaurantResposneDTO> getRestaurantByIdWithMetrics(
        @Param("id") Integer id
    );

    @Query("""
        SELECT new com.yamiapp.model.dto.RestaurantResposneDTO(
            r.id, r.name, r.shortName, r.description,
            COALESCE(COUNT(DISTINCT f.id), 0),
            COALESCE(COUNT(DISTINCT fr.id), 0)
        )
        FROM Restaurant r
        LEFT JOIN r.foods f
        LEFT JOIN FoodReview fr ON fr.food.id = f.id
        WHERE r.name ILIKE CONCAT('%', :searchParams, '%')
        GROUP BY r.id, r.name, r.shortName, r.description
    """)
    Page<RestaurantResposneDTO> getRestaurantsByAnonymousSearch(
        @Param("searchParams") String searchParams,
        Pageable pageable
    );

}