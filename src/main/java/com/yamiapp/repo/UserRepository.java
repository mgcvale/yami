package com.yamiapp.repo;

import com.yamiapp.model.User;
import com.yamiapp.model.dto.RatingDistributionEntry;
import com.yamiapp.model.dto.UserCountsDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    @Query("select u from User u where u.accessToken = :accessToken")
    Optional<User> findByAccessToken(@Param("accessToken") String accessToken);

    @Query("select u from User u where u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("select u from User u where u.username = :username or u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    @Query("select u from User u where u.id = :id")
    Optional<User> findById(@Param("id") Long id);


    @Query(value = """
        select
            (select COUNT(*) from follows where following_id = :userId) as followerCount,
            (select COUNT(*) from follows where follower_id = :userId) as followingCount,
            (select COUNT(*) from food_reviews where user_id = :userId) as reviewCount
       """, nativeQuery = true)
    UserCountsDTO getUserCounts(@Param("userId") Long userId);

    @Query(value = """
        select r.rating as k, COUNT(*) as v\
        from food_reviews r\
        where r.user_id = :userId\
        group by r.rating\
    """, nativeQuery = true)
    List<RatingDistributionEntry> getRatingDistribution(@Param("userId") Long userId);

    @Query("SELECT AVG(r.rating) FROM FoodReview r WHERE r.user.id = :userId")
    Double getAverageRating(@Param("userId") Long userId);
}
