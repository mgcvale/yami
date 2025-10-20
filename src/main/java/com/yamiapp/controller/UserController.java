package com.yamiapp.controller;

import com.yamiapp.model.FoodReview;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.*;
import com.yamiapp.service.FoodReviewService;
import com.yamiapp.service.UserFollowService;
import com.yamiapp.service.UserService;
import com.yamiapp.util.ControllerUtils;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final UserFollowService userFollowService;
    private final FoodReviewService foodReviewService;

    public UserController(UserService userService, UserFollowService userFollowService, FoodReviewService foodReviewService) {
        this.userService = userService;
        this.userFollowService = userFollowService;
        this.foodReviewService = foodReviewService;
    }

    @PostMapping("")
    public ResponseEntity<Object> createUser(@RequestBody UserDTO user) {
        User u = userService.createRawUser(user);
        return ResponseEntity.ok(new UserResponseDTO(u).withToken(u.getAccessToken()).withCounts(userService.getUserCounts(u.getId())));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader, @RequestBody(required = false) UserLoginDTO loginInfo) {
        if (authHeader == null) {
            User u = userService.getRawByPassword(loginInfo);
            return ResponseEntity.ok().body(new UserResponseDTO(u).withToken(u.getAccessToken()).withCounts(userService.getUserCounts(u.getId())));
        }

        String token = ControllerUtils.extractToken(authHeader);
        UserResponseDTO user = userService.getByToken(token);
        return ResponseEntity.ok().body(user.withCounts(userService.getUserCounts(user.getId())).withToken(token));
    }

    @PatchMapping("")
    public ResponseEntity<Object> editUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader, @RequestBody UserDTO user) {
        String token = ControllerUtils.extractToken(authHeader);
        User u = userService.updateRawUser(token, user);
        return ResponseEntity.ok().body(new UserResponseDTO(u).withCounts(userService.getUserCounts(u.getId())).withToken(u.getAccessToken()));
    }

    @DeleteMapping("")
    public ResponseEntity<Object> deleteUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader, @RequestBody UserLoginDTO loginInfo) {
        String token = ControllerUtils.extractToken(authHeader);
        userService.deleteUser(token, loginInfo);
        return ResponseFactory.createSuccessResponse(MessageStrings.USER_DELETE_SUCCESS.getMessage());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUser(@PathVariable(value = "id") Long id, @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        if (authHeader == null) return ResponseEntity.ok().body(userService.getById(id).withoutSensitiveData().withCounts(userService.getUserCounts(id)));

        String token = ControllerUtils.extractToken(authHeader);
        return ResponseEntity.ok().body(userService.getById(id).withoutSensitiveData().withCounts(userService.getUserCounts(id)).withFollowing(userFollowService.isFollowingByToken(token, id)));
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Object> getUserStats(@PathVariable Long id) {
        return ResponseEntity.ok().body(userService.getUserStats(id));
    }

    @GetMapping("/search/{searchParams}")
    public ResponseEntity<Object> getUserBySearch(
            @PathVariable() String searchParams,
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(defaultValue =  "0") Integer offset,
            @RequestParam(defaultValue =  "50") Integer count
    ) {
        if (authHeader == null) return ResponseEntity.ok().body(userService.searchUsersUnauthenticated(searchParams, Pageable.ofSize(count).withPage(offset)));
        return ResponseEntity.ok().body(userService.searchUsersAuthenticated(
                ControllerUtils.extractToken(authHeader),
                searchParams,
                Pageable.ofSize(count).withPage(offset)
        ));
    }

    @GetMapping("/feed")
    public ResponseEntity<Page<FoodReviewResponseDTO>> getUserFeed(
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
        @RequestParam(defaultValue =  "0") Integer offset,
        @RequestParam(defaultValue =  "20") Integer count
    ) {
        String token = ControllerUtils.extractToken(authHeader);
        Pageable pageable = Pageable.ofSize(count).withPage(offset);

        return ResponseEntity.ok(foodReviewService.getFoodReviewsByFollowers(token, pageable));
    }
}