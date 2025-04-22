package com.yamiapp.controller;

import com.yamiapp.model.FoodReview;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.FoodReviewResponseDTO;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.model.dto.UserResponseDTO;
import com.yamiapp.service.FoodReviewService;
import com.yamiapp.service.UserService;
import com.yamiapp.util.ControllerUtils;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private final FoodReviewService foodReviewService;

    public UserController(UserService userService, FoodReviewService foodReviewService) {
        this.userService = userService;
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
        return ResponseEntity.ok().body(user.withCounts(userService.getUserCounts(user.getId())));
    }

    @PatchMapping("")
    public ResponseEntity<Object> editUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader, @RequestBody UserDTO user) {
        String token = ControllerUtils.extractToken(authHeader);
        userService.updateRawUser(token, user);
        return ResponseFactory.createSuccessResponse(MessageStrings.USER_EDIT_SUCCESS.getMessage());
    }

    @DeleteMapping("")
    public ResponseEntity<Object> deleteUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader, @RequestBody UserLoginDTO loginInfo) {
        String token = ControllerUtils.extractToken(authHeader);
        userService.deleteUser(token, loginInfo);
        return ResponseFactory.createSuccessResponse(MessageStrings.USER_DELETE_SUCCESS.getMessage());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUser(@PathVariable(value = "id") Long id) {
        return ResponseEntity.ok().body(userService.getById(id).withoutSensitiveData().withCounts(userService.getUserCounts(id)));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<Object> getUserReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "50") Integer count,
            @RequestParam(required = false, defaultValue = "") String keyword
    ) {
        Page<FoodReview> foodReviews = foodReviewService.getFoodReviewsByUser(id, "", keyword, Pageable.ofSize(count).withPage(offset));
        Page<FoodReviewResponseDTO> responseReviews = foodReviews.map(FoodReviewResponseDTO::new);
        return ResponseEntity.ok().body(responseReviews);
    }

}
