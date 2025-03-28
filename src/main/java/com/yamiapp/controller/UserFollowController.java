package com.yamiapp.controller;


import com.yamiapp.service.UserFollowService;
import com.yamiapp.util.ControllerUtils;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user/follow")
public class UserFollowController {

    private final UserFollowService userFollowService;

    public UserFollowController(UserFollowService userFollowService) {
        this.userFollowService = userFollowService;
    }

    @PostMapping("/{id}")
    public ResponseEntity<Object> createFollow(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        String token = ControllerUtils.extractToken(authHeader);
        userFollowService.follow(token, id);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOLLOW_CREATE_SUCCESS.getMessage());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteFollow(
            @RequestHeader(value = "Authorization", required = false) String authHeader,
            @PathVariable Long id
    ) {
        String token = ControllerUtils.extractToken(authHeader);
        userFollowService.unfollow(token, id);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOLLOW_DELETE_SUCCESS.getMessage());
    }

    @GetMapping("/{id}/followers")
    public ResponseEntity<Object> getFollowers(@PathVariable Long id) {
        return ResponseEntity.ok().body(userFollowService.getFollowers(id));
    }

    @GetMapping("/{id}/following")
    public ResponseEntity<Object> getFollowing(@PathVariable Long id) {
        return ResponseEntity.ok().body(userFollowService.getFollowing(id));
    }

    @GetMapping("/{follower}/following/{followed}")
    public ResponseEntity<Object> isFollowing(
            @PathVariable Long follower,
            @PathVariable Long followed
    ) {
        boolean isFollowing = userFollowService.isFollowing(follower, followed);
        return ResponseEntity.ok().body(Map.of("following", isFollowing));
    }
}
