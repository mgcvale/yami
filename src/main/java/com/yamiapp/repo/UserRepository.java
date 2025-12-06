package com.yamiapp.repo;

import com.yamiapp.model.User;
import com.yamiapp.model.dto.RatingDistributionEntry;
import com.yamiapp.model.dto.UserCountsDTO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    @Query("select u from User u where u.email = :email")
    Optional<User> findByEmail(@Param("email") String email);

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
        from food_reviews r
        where r.user_id = :userId
        group by r.rating
    """, nativeQuery = true)
    List<RatingDistributionEntry> getRatingDistribution(@Param("userId") Long userId);

    @Query("select AVG(r.rating) from FoodReview r where r.user.id = :userId")
    Double getAverageRating(@Param("userId") Long userId);

    @Query("select u from User u where u.username ilike :searchParams")
    Page<User> getUsersByAnonymousSearch(@Param("searchParams") String searchParams, Pageable pageable);

    @Query("""
        SELECT DISTINCT f2
        FROM User u
        JOIN u.following f1
        JOIN f1.following f2
        WHERE u.id = :userId
        AND LOWER(f2.username) iLIKE LOWER(:search)
        AND f2.id <> :userId
    """)

    Page<User> findSecondDegree(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    @Query("""
        SELECT DISTINCT u
        FROM User u
        JOIN u.following f
        WHERE f IN (
            SELECT f1 FROM User u2 JOIN u2.following f1 WHERE u2.id = :userId
        )
        AND u.id <> :userId
        AND LOWER(u.username) LIKE LOWER(:search)
    """)
    Page<User> findSharedInterest(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

    @Query(value = """
        SELECT u.*
        FROM users u
        LEFT JOIN follows f ON f.following_id = u.user_id
        WHERE u.user_id NOT IN (
            SELECT following_id FROM follows WHERE follower_id = :userId
        ) AND u.user_id <> :userId
        AND LOWER(u.username) LIKE LOWER(:search)
        GROUP BY u.user_id
        ORDER BY COUNT(f.follower_id) DESC
    """, nativeQuery = true)
    Page<User> findPopularUsersExcludingFollows(@Param("userId") Long userId, @Param("search") String search, Pageable pageable);

}
