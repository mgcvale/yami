package com.yamiapp.controller;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.UnauthorizedException;
import com.yamiapp.model.dto.RestaurantDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.service.RestaurantService;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.apache.coyote.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/restaurant")
public class RestaurantController {

    private final RestaurantService restaurantService;

    public RestaurantController(RestaurantService restaurantService) {
        this.restaurantService = restaurantService;
    }


    @PostMapping
    public ResponseEntity<Object> createRestaurant(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "photo", required = false) MultipartFile photo) throws B2Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        RestaurantDTO dto = new RestaurantDTO(name, photo, description);
        restaurantService.createRestaurant(dto, token);

        return ResponseFactory.createSuccessResponse(MessageStrings.RESTAURANT_CREATE_SUCCESS.getMessage());
    }

    @PatchMapping
    public ResponseEntity<Object> updateRestaurant(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam("id") Integer id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "photo", required = false) MultipartFile photo) throws B2Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        RestaurantDTO dto = RestaurantDTO.builder().name(name).description(description).photo(photo).build();
        restaurantService.updateRestaurant(id, dto, token);

        return ResponseFactory.createSuccessResponse(MessageStrings.RESTAURANT_UPDATE_SUCCESS.getMessage());
    }

    @DeleteMapping
    public ResponseEntity<Object> deleteRestaurant(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password") String password,
            @RequestParam("id") Integer id
    ) throws B2Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        UserLoginDTO dto = UserLoginDTO.builder().email(email).username(username).password(password).build();

        restaurantService.deleteRestaurant(id, token, dto);

        return ResponseFactory.createSuccessResponse(MessageStrings.RESTAURANT_DELETE_SUCCESS.getMessage());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getRestaurantData(@PathVariable Integer id) {
        return ResponseEntity.ok().body(restaurantService.getById(id));
    }

    @GetMapping("/{id}/picture")
    public ResponseEntity<Object> getRestaurantImage(@PathVariable Integer id) {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(restaurantService.getImageById(id));
    }

}
