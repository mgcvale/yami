package com.food.project.controller;

import com.food.project.exception.*;
import com.food.project.model.User;
import com.food.project.model.dto.UserDTO;
import com.food.project.model.dto.UserLoginDTO;
import com.food.project.model.dto.UserResponseDTO;
import com.food.project.service.UserService;
import com.food.project.util.MessageStrings;
import com.food.project.util.ResponseFactory;
import org.hibernate.Internal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;
    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("")
    public ResponseEntity<Object> createUser(@RequestBody UserDTO user) {
        var u = userService.createUser(user);
        return ResponseEntity.ok(u);
    }


    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@RequestBody UserLoginDTO loginInfo) {
        User u = userService.getByPassword(loginInfo);
        return ResponseEntity.ok().body(new UserResponseDTO(u));

    }

    @PatchMapping("")
    public ResponseEntity<Object> editUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader, @RequestBody UserDTO user) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException("Invalid access token"), 401);
        }
        String token = authHeader.substring(7);

        userService.updateUser(token, user);
        return ResponseFactory.createSuccessResponse(MessageStrings.USER_EDIT_SUCCESS.getMessage());
    }

}
