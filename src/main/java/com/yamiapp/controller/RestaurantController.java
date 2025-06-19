package com.yamiapp.controller;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.yamiapp.model.dto.RestaurantDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.service.RestaurantService;
import com.yamiapp.util.ControllerUtils;
import com.yamiapp.util.MessageStrings;
import com.yamiapp.util.ResponseFactory;
import org.springframework.data.domain.Pageable;
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
            @RequestParam(value = "photo", required = false) MultipartFile photo,
            @RequestParam(value = "shortName", required = false) String shortName) throws B2Exception {
        String token = ControllerUtils.extractToken(authHeader);

        RestaurantDTO dto = new RestaurantDTO(name, shortName, photo, description);
        restaurantService.createRestaurant(dto, token);

        return ResponseFactory.createSuccessResponse(MessageStrings.RESTAURANT_CREATE_SUCCESS.getMessage());
    }

    @PatchMapping("/{id}")
    public ResponseEntity<Object> updateRestaurant(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @PathVariable Integer id,
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "description", required = false) String description,
            @RequestParam(value = "shortName", required = false) String shortName,
            @RequestParam(value = "photo", required = false) MultipartFile photo) throws B2Exception {
        String token = ControllerUtils.extractToken(authHeader);

        RestaurantDTO dto = RestaurantDTO.builder().name(name).description(description).photo(photo).shortName(shortName).build();
        restaurantService.updateRestaurant(id, dto, token);

        return ResponseFactory.createSuccessResponse(MessageStrings.RESTAURANT_UPDATE_SUCCESS.getMessage());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Object> deleteRestaurant(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "password") String password,
            @PathVariable Integer id
    ) throws B2Exception {
        String token = ControllerUtils.extractToken(authHeader);
        UserLoginDTO dto = UserLoginDTO.builder().email(email).username(username).password(password).build();

        restaurantService.deleteRestaurant(id, token, dto);
        return ResponseFactory.createSuccessResponse(MessageStrings.RESTAURANT_DELETE_SUCCESS.getMessage());
    }

    @GetMapping("/search/{searchParams}")
    public ResponseEntity<Object> searchRestaurantsUnauthenticated(
        @PathVariable(value = "searchParams") String searchPrams,
        @RequestParam(defaultValue =  "0") Integer offset,
        @RequestParam(defaultValue =  "50") Integer count
    ) {
        // no need for token here
        return ResponseEntity.ok().body(restaurantService.searchRestaurantsUnauthenticated(searchPrams, Pageable.ofSize(count).withPage(offset)));
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
