package com.yamiapp.controller;

import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.UnauthorizedException;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.model.dto.UserResponseDTO;
import com.yamiapp.service.UserService;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
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
        return ResponseEntity.ok(new UserResponseDTO(u).withToken(u.getAccessToken()));
    }

    @PostMapping("/login")
    public ResponseEntity<Object> loginUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader, @RequestBody(required = false) UserLoginDTO loginInfo) {
        if (authHeader == null) {
            User u = userService.getByPassword(loginInfo);
            return ResponseEntity.ok().body(new UserResponseDTO(u).withToken(u.getAccessToken()));
        }

        if (!authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        return ResponseEntity.ok().body(new UserResponseDTO(userService.getByToken(token)).withToken(token));
    }


    @PatchMapping("")
    public ResponseEntity<Object> editUser(@RequestHeader(HttpHeaders.AUTHORIZATION) String authHeader, @RequestBody UserDTO user) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        userService.updateUser(token, user);
        return ResponseFactory.createSuccessResponse(MessageStrings.USER_EDIT_SUCCESS.getMessage());
    }

    @DeleteMapping("")
    public ResponseEntity<Object> deleteUser(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader, @RequestBody UserLoginDTO loginInfo) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        userService.deleteUser(token, loginInfo);
        return ResponseFactory.createSuccessResponse(MessageStrings.USER_DELETE_SUCCESS.getMessage());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getUser(@PathVariable(value = "id") Long id) {
        return ResponseEntity.ok().body(userService.getById(id).withoutSensitiveData());
    }

}
