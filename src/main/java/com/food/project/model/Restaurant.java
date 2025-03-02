package com.food.project.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Table(name = "restaurants")
@Entity
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long id;

    @Setter
    @Column(nullable = false, name = "name")
    private String name;

    @Setter
    @Column(nullable = true, name = "photo")
    private String photoPath;

    @Setter
    @Column(nullable = false, name = "description")
    private String description;

}
