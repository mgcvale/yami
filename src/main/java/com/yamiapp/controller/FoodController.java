package com.yamiapp.controller;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.UnauthorizedException;
import com.yamiapp.model.dto.FoodDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.service.FoodService;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/food")
public class FoodController {

    private final FoodService foodService;

    public FoodController(FoodService foodService) {
        this.foodService = foodService;
    }

    @PostMapping
    public ResponseEntity<Object> createFood(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "description") String description,
            @RequestParam(value = "restaurantId") Integer restaurantId,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) throws B2Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        FoodDTO foodDTO = FoodDTO.builder().restaurantId(restaurantId).name(name).description(description).photo(photo).build();

        foodService.createFood(foodDTO, token);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOOD_CREATE_SUCCESS.getMessage());
    }

    @PatchMapping
    public ResponseEntity<Object> updateFood(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "foodId", required = true) Integer foodId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "restaurantId", required = false) Integer restaurantId,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) throws B2Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        FoodDTO foodDTO = FoodDTO.builder().restaurantId(restaurantId).name(name).description(description).photo(photo).build();

        foodService.updateFood(foodId, foodDTO, token);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOOD_UPDATE_SUCCESS.getMessage());
    }

    @DeleteMapping
    public ResponseEntity<Object> deleteFood(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            @RequestParam(value = "foodId") Integer foodId
    ) throws B2Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        UserLoginDTO loginDTO = new UserLoginDTO(username, email, password);
        foodService.deleteFood(foodId, token, loginDTO);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOOD_DELETE_SUCCESS.getMessage());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Object> getById(@PathVariable Integer id) {
        return ResponseEntity.ok().body(foodService.getById(id));
    }

    @GetMapping("/{id}/picture")
    public ResponseEntity<Object> getPictureById(@PathVariable Integer id) throws B2Exception {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(foodService.getImageById(id));
    }


}
