package com.yamiapp.repo;

import com.yamiapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserFollowRepository extends JpaRepository<User, Long> {
    Set<User> findByFollowing(User u); // the 'by' is just for jparepository syntax, assume the real name would be "findFollowing" and "findFollowers"
    Set<User> findByFollowers(User u);
}
