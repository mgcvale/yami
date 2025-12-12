package com.yamiapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Table(
    name = "review_like",
    uniqueConstraints = @UniqueConstraint(columnNames = {"post_id", "user_id"})
)
@Entity
public class ReviewLike {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_like_id")
    private Long reviewLikeId;

    @Setter
    @ManyToOne
    @JoinColumn(name = "food_review_id", nullable = false)
    private FoodReview review;

    @Setter
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private LocalDateTime likedAt = LocalDateTime.now();
}
