package com.yamiapp.repo;

import com.yamiapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Integer> {
    @Query("select u from User u where u.accessToken = :accessToken")
    Optional<User> findByAccessToken(@Param("accessToken") String accessToken);

    @Query("select u from User u where u.username = :username")
    Optional<User> findByUsername(@Param("username") String username);

    @Query("select u from User u where u.username = :username or u.email = :email")
    Optional<User> findByUsernameOrEmail(@Param("username") String username, @Param("email") String email);

    @Query("select u from User u where u.id = :id")
    Optional<User> findById(@Param("id") Long id);
}