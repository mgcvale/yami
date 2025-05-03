package com.yamiapp.repo;

import com.yamiapp.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Set;

@Repository
public interface UserFollowRepository extends JpaRepository<User, Long> {
    Set<User> findByFollowing(User u); // the 'by' is just for jparepository syntax, assume the real name would be "findFollowing" and "findFollowers"
    Set<User> findByFollowers(User u);

    @Query("select count(u.followers) from User u where u.id=:userId")
    Long countFollowerById(Long userId);

    @Query("select count(u.following) from User u where u.id=:userId")
    Long countFollowingById(Long userId);

    @Query("select case when count(f) > 0 then true else false end " +
            "from User u join u.following f " +
            "where u.accessToken = :accessToken and f.id = :targetId")
    boolean existsFollowingByAccessTokenAndTargetId(String accessToken, Long targetId);

    @Query("select case when count(f) > 0 then true else false end " +
            "from User u join u.following f " +
            "where u.id = :followerId and f.id = :targetId")
    boolean existsFollowingByUserIdAndTargetId(Long followerId, Long targetId);
}
