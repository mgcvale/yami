package com.yamiapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "food_id"})
)
public class Review {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @Column(name = "review", length = 1023)
    private String review;

    @Column(name = "rating")
    private Float rating;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", referencedColumnName = "user_id")
    private User user;

    @OneToOne(cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinColumn(name = "food_id", referencedColumnName = "food_id")
    private Food food;

    @PrePersist
    @PreUpdate
    private void validateRating() {
        if (rating < 0 || rating > 10 || rating * 2 % 1 != 0) {
            throw new IllegalArgumentException("Rating must be between 0 and 10 in 0.5 increments.");
        }
    }
}
