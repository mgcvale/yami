package com.yamiapp.controller;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.UnauthorizedException;
import com.yamiapp.model.dto.FoodDTO;
import com.yamiapp.service.FoodService;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.springframework.http.HttpHeaders;
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
    public ResponseEntity<Object> createUser(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "description") String description,
            @RequestParam(value = "restaurantId") Integer restaurantId,
            @RequestParam(value = "photo", required = false)MultipartFile photo
    ) throws B2Exception {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage()), 401);
        }
        String token = authHeader.substring(7);

        FoodDTO foodDTO = FoodDTO.builder().restaurantId(restaurantId).name(name).description(description).photo(photo).build();

        foodService.createFood(foodDTO, token);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOOD_CREATE_SUCCESS.getMessage());
    }

    

}
