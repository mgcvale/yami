package com.yamiapp.model;

import jakarta.persistence.*;
import lombok.Getter;

@Table(
    name="restaurant_follow",
    uniqueConstraints = @UniqueConstraint(columnNames = {"restaurant_id", "user_id"})
)
@Entity
@Getter
public class RestaurantFollow {
    @Id
    @Column
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long restaurantFollowId;

    @ManyToOne
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
}
