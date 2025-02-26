package com.food.project.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;

import java.util.HashSet;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "users")
@Getter
@Setter
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_id")
    private Long id;

    @Column(nullable = false, name = "username", unique = true)
    private String username;

    @Column(nullable = true, name = "bio", length = 128)
    private String bio;

    @Column(nullable = true, name = "location")
    private String location;

    @Column(nullable = false, name = "password_hash")
    private String passwordHash;

    @Column(nullable = false, name = "access_token", unique = true)
    private String accessToken;

    @Column(nullable = false, name = "email", unique = true)
    private String email;

    @JsonIgnore
    @Column(nullable = false, name = "role")
    private Role role;

    // FKs
    @ManyToMany
    @JoinTable(
            name = "follow",
            joinColumns = @JoinColumn(name = "follower_id"),
            inverseJoinColumns = @JoinColumn(name = "following_id")
    )
    private Set<User> following = new HashSet<>();

    @ManyToMany(mappedBy = "following")
    private Set<User> followers = new HashSet<>();

    public User(Long id, String username, String bio, String location, String passwordHash, String accessToken, String email, Set<User> following, Set<User> followers) {
        this.id = id;
        this.username = username;
        this.bio = bio;
        this.location = location;
        this.passwordHash = passwordHash;
        this.accessToken = accessToken;
        this.following = following;
        this.followers = followers;
        this.email = email;
        this.role = Role.USER;
    }

    public User() {}
}

