package com.yamiapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Table(
        name = "foods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "restaurant_id"})
)
@Entity
public class Food {
    public Food() {
        avgRating = 0D;
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "food_id")
    private Long id;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "description", nullable = false, length = 511)
    private String description;

    @Setter
    @Column(nullable = true, name = "photo")
    private String photoPath;

    @Setter
    @Column(nullable = true, name = "photo_id")
    private String photoId;

    @Setter
    @Column(name = "avg_rating")
    private Double avgRating;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
}
