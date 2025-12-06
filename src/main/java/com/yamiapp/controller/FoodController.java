package com.yamiapp.controller;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.yamiapp.model.Food;
import com.yamiapp.model.FoodReview;
import com.yamiapp.model.dto.FoodDTO;
import com.yamiapp.model.dto.FoodResponseDTO;
import com.yamiapp.model.dto.FoodReviewResponseDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.service.FoodReviewService;
import com.yamiapp.service.FoodService;
import com.yamiapp.service.UserService;
import com.yamiapp.util.ControllerUtils;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/food")
public class FoodController {

    private final FoodService foodService;
    private final FoodReviewService foodReviewService;

    public FoodController(FoodService foodService, final FoodReviewService foodReviewService) {
        this.foodService = foodService;
        this.foodReviewService = foodReviewService;
    }

    @PostMapping
    public ResponseEntity<Object> createFood(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "description") String description,
            @RequestParam(value = "restaurantId") Integer restaurantId,
            @RequestParam(value = "photo", required = false) MultipartFile photo
    ) throws B2Exception {
        String token = ControllerUtils.extractToken(authHeader);

        FoodDTO foodDTO = FoodDTO.builder().restaurantId(restaurantId).name(name).description(description).photo(photo).build();

        Food f = foodService.createFood(foodDTO, token);
        return ResponseEntity.ok().body(new FoodResponseDTO(f));
    }

    @PatchMapping("/{foodId}")
    public ResponseEntity<Map<String, String>> updateFood(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable Long foodId,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "restaurantId", required = false) Integer restaurantId,
            @RequestParam(required = false) MultipartFile photo
    ) throws B2Exception {
        String token = ControllerUtils.extractToken(authHeader);

        FoodDTO foodDTO = FoodDTO.builder().restaurantId(restaurantId).name(name).description(description).photo(photo).build();
        System.out.println(foodDTO.getPhoto());
        foodService.updateFood(foodId, foodDTO, token);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOOD_UPDATE_SUCCESS.getMessage());
    }

    @DeleteMapping("/{foodId}")
    public ResponseEntity<Map<String, String>> deleteFood(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password", required = false) String password,
            @PathVariable Long foodId
    ) throws B2Exception {
        String token = ControllerUtils.extractToken(authHeader);

        UserLoginDTO loginDTO = new UserLoginDTO(username, email, password);
        foodService.deleteFood(foodId, token, loginDTO);
        return ResponseFactory.createSuccessResponse(MessageStrings.FOOD_DELETE_SUCCESS.getMessage());
    }

    @GetMapping("/{id}")
    public ResponseEntity<FoodResponseDTO> getById(
        @PathVariable Long id,
        @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader
    ) {
        if (authHeader == null) {
            return ResponseEntity.ok().body(foodService.getById(id));
        }

        String token = ControllerUtils.extractToken(authHeader);
        return ResponseEntity.ok().body(foodService.getByIdAuthenticated(id, token));
    }


    @GetMapping("/{id}/picture")
    public ResponseEntity<Object> getPictureById(@PathVariable Long id) throws B2Exception {
        return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(foodService.getImageById(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<Object> getAllFoodReviews(
            @PathVariable Long id,
            @RequestParam(defaultValue = "0") Integer offset,
            @RequestParam(defaultValue = "50") Integer count,
            @RequestParam(required = false, defaultValue = "") String keyword
    ) {
        Page<FoodReview> foodReviews = foodReviewService.getFoodReviewsByFoodId(id, keyword, Pageable.ofSize(count).withPage(offset));
        Page<FoodReviewResponseDTO> responseReviews = foodReviews.map(FoodReviewResponseDTO::new);
        return ResponseEntity.ok().body(responseReviews);
    }

    @GetMapping("/{id}/average_rating")
    public ResponseEntity<Object> getAverageRating(
            @PathVariable Long id
    ) {
        double avg = foodService.getAverageRating(id);
        return new ResponseFactory.JsonResponseChain().add("average", avg).build();
    }

    @GetMapping("/{id}/stats")
    public ResponseEntity<Object> getFoodStats(@PathVariable Long id) {
        return ResponseEntity.ok().body(foodService.getFoodStats(id));
    }

    @GetMapping("/by_restaurant/{id}")
    public ResponseEntity<Object> getFoodsByRestaurant(@PathVariable Long id) {
        return ResponseEntity.ok().body(foodService.getByRestaurantId(id));
    }

    @GetMapping("/by_restaurant/{id}/search/{query}")
    public ResponseEntity<Object> getFoodsByRestaurantAndQuery(@PathVariable Long id, @PathVariable String query) {
        return ResponseEntity.ok().build();
    }

}
