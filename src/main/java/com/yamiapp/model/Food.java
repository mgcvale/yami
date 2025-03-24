package com.yamiapp.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.Set;

@Getter
@Setter
@Table(
        name = "foods",
        uniqueConstraints = @UniqueConstraint(columnNames = {"name", "restaurant_id"})
)
@Entity
public class Food {
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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id")
    private Restaurant restaurant;
}
