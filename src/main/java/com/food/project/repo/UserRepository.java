package com.food.project.repo;

import com.food.project.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("select u from User u where u.accessToken = :accessToken")
    Optional<User> findByAccessToken(@Param("accessToken") String accessToken);

    @Query("select u from User u where u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);
}