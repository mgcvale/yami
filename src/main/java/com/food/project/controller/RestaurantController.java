package com.food.project.controller;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.food.project.exception.ErrorStrings;
import com.food.project.exception.UnauthorizedException;
import com.food.project.model.dto.RestaurantDTO;
import com.food.project.service.RestaurantService;
import com.food.project.util.MessageStrings;
import com.food.project.util.ResponseFactory;
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
            @RequestParam("name") String name,
            @RequestParam("description") String description,
            @RequestParam("photo") MultipartFile photo) throws B2Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        RestaurantDTO dto = new RestaurantDTO(name, photo, description);
        restaurantService.createRestaurant(dto, token);

        return ResponseFactory.createSuccessResponse(MessageStrings.RESTAURANT_CREATE_SUCCESS.getMessage());
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
