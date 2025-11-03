package com.yamiapp.repo;

import com.yamiapp.model.Restaurant;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.RestaurantResposneDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
public interface RestaurantRepository extends JpaRepository<Restaurant, Integer> {

    @Query("""
        SELECT new com.yamiapp.model.dto.RestaurantResposneDTO(
            r.id, r.name, r.shortName, r.description,
            CAST(COALESCE(COUNT(DISTINCT f.id), 0L) AS LONG),
            CAST(COALESCE(COUNT(DISTINCT fr.id), 0L) AS LONG)
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
            CAST(COALESCE(COUNT(DISTINCT f.id), 0L) AS LONG),
            CAST(COALESCE(COUNT(DISTINCT fr.id), 0L) AS LONG)
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

    @Query("""
        SELECT new com.yamiapp.model.dto.RestaurantResposneDTO(
            r.id, r.name, r.shortName, r.description,
            CAST(COALESCE(COUNT(DISTINCT f.id), 0L) AS LONG),
            CAST(COALESCE(COUNT(DISTINCT fr.id), 0L) AS LONG)
        )
        from Restaurant r
        LEFT JOIN r.foods f
        LEFT JOIN FoodReview fr ON fr.food.id = f.id
        WHERE fr.user IN (
            SELECT u from User u WHERE :user MEMBER OF u.following
        )
        GROUP BY r.id, r.name, r.shortName, r.description
        ORDER BY COUNT(DISTINCT fr.id) DESC
        LIMIT 100
    """)
    List<RestaurantResposneDTO> findRestaurantReccomendations(@Param("user") User u);

}