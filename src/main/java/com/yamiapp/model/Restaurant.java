package com.yamiapp.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Table(name = "restaurants")
@Entity
public class Restaurant {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "restaurant_id")
    private Long id;

    @Setter
    @Column(nullable = false, name = "name", unique = true)
    private String name;

    @Setter
    @Column(nullable = true, name="short_name", unique = false)
    private String shortName;

    @Setter
    @Column(nullable = true, name = "photo")
    private String photoPath;

    @Setter
    @Column(nullable = true, name = "photo_id")
    private String photoId;

    @Setter
    @Column(nullable = false, name = "description")
    private String description;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Food> foods = new ArrayList<>();
}
