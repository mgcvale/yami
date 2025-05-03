package com.yamiapp.service;

import com.yamiapp.controller.UserFollowController;
import com.yamiapp.exception.BadRequestException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserResponseDTO;
import com.yamiapp.repo.UserFollowRepository;
import com.yamiapp.repo.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class UserFollowService {

    private final UserRepository userRepository;
    private final UserFollowRepository userFollowRepository;
    private final UserService userService;

    public UserFollowService(
            final UserRepository userRepository,
            final UserFollowRepository userFollowRepository,
            UserService userService) {
        this.userRepository = userRepository;
        this.userFollowRepository = userFollowRepository;
        this.userService = userService;
    }

    @Transactional
    public void follow(String followerToken, Long followedId) {
        User follower = userService.getRawByToken(followerToken);
        User followed = userService.getRawById(followedId);

        if (followed.getId().equals(follower.getId())) {
            throw new BadRequestException(ErrorStrings.CANNOT_FOLLOW_ONESELF.getMessage());
        }

        followed.getFollowers().add(follower);
        follower.getFollowing().add(followed);

        userRepository.save(followed);
        userRepository.save(follower);
    }

    public void unfollow(String unfollowerToken, Long unfollowedId) {
        User unfollower = userService.getRawByToken(unfollowerToken);
        User unfollowed = userService.getRawById(unfollowedId);

        unfollower.getFollowing().remove(unfollowed);
        unfollowed.getFollowers().remove(unfollower);

        userRepository.save(unfollowed);
        userRepository.save(unfollower);
    }

    public Set<UserResponseDTO> getFollowers(Long userId) {
        User user = userService.getRawById(userId);
        Set<User> followers = user.getFollowers();

        Set<UserResponseDTO> followersMapped = followers.stream().map(u -> new UserResponseDTO(u).withoutSensitiveData()).collect(Collectors.toSet());
        return followersMapped;
    }

    public Set<UserResponseDTO> getFollowing(Long userId) {
        User user = userService.getRawById(userId);
        Set<User> following = user.getFollowing();

        Set<UserResponseDTO> followingMapped = following.stream().map(u -> new UserResponseDTO(u).withoutSensitiveData()).collect(Collectors.toSet());
        return followingMapped;
    }

    public boolean isFollowing(Long followerId, Long followedId) {
        return userFollowRepository.existsFollowingByUserIdAndTargetId(followerId, followedId);
    }

    public boolean isFollowingByToken(String followerToken, Long followedId) {
        return userFollowRepository.existsFollowingByAccessTokenAndTargetId(followerToken, followedId);
    }


}
