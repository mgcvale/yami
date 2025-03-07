package com.yamiapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamiapp.config.TestConfig;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.mock.FakeBackblazeService;
import com.yamiapp.model.Food;
import com.yamiapp.model.Restaurant;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.RestaurantDTO;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.repo.FoodRepository;
import com.yamiapp.repo.RestaurantRepository;
import com.yamiapp.repo.UserRepository;
import com.yamiapp.service.RestaurantService;
import com.yamiapp.util.MessageStrings;
import io.github.cdimascio.dotenv.Dotenv;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;


import static org.junit.jupiter.api.Assertions.*;
import static com.yamiapp.util.TestUtils.createUserWithRole;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class FoodControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private FoodRepository foodRepository;

    @Autowired
    private FakeBackblazeService backblazeService;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserDTO adminUser = new UserDTO("adminuser", "adminpassword123", "admin bio", "admin location", "admin@example.com");
    private final UserDTO regularUser = new UserDTO("regularuser", "userpassword123", "user bio", "user location", "user@example.com");
    private final UserDTO moderatorUser = new UserDTO("moderatoruser", "modpassword123", "mod bio", "mod location", "mod@example.com");
    private RestaurantDTO restaurantDTO;

    private User createdAdminUser;
    private User createdRegularUser;
    private User createdModeratorUser;
    private Restaurant createdRestaurant;
    private byte[] testImageBytes;
    private RestaurantService restaurantService;

    @BeforeAll
    public static void initialize() {
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(dotenvEntry -> System.setProperty(dotenvEntry.getKey(), dotenvEntry.getValue()));
    }

    @BeforeEach
    public void setup() throws Exception {
        createdAdminUser = createUserWithRole(mockMvc, objectMapper, userRepository, adminUser, Role.ADMIN);
        createdRegularUser = createUserWithRole(mockMvc, objectMapper, userRepository, regularUser, Role.USER);
        createdModeratorUser = createUserWithRole(mockMvc, objectMapper, userRepository, moderatorUser, Role.MODERATOR);
        backblazeService = new FakeBackblazeService();

        try {
            testImageBytes = Files.readAllBytes(Paths.get("src/test/resources/test-image.png"));
        } catch (IOException e) {
            testImageBytes = new byte[1024]; // this will probably break some tests
        }
        MockMultipartFile restaurantPhoto = new MockMultipartFile(
                "photo",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                testImageBytes
        );
        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(restaurantPhoto)
                        .param("name", "RestaurantName")
                        .param("description", "RestaurantDescription")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isOk());
        createdRestaurant = restaurantRepository.findAll().getFirst();
    }

    @AfterEach
    public void cleanup() throws Exception {
        restaurantRepository.deleteAll();
        userRepository.deleteAll();
        foodRepository.deleteAll();
        backblazeService.deleteAll();
    }

    @Test
    public void testCreateFoodWithAdminUserSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazillian-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_CREATE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 1);
        Food f = foods.getFirst();
        assertEquals(f.getName(), "strogonoff");
        assertEquals(f.getDescription(), "delicious brazillian-style strogonoff");
        assertEquals(f.getRestaurant().getId(), createdRestaurant.getId());
    }


    @Test
    public void testCreateFoodWithModeratorSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdModeratorUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazillian-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_CREATE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 1);
        Food f = foods.getFirst();
        assertEquals(f.getName(), "strogonoff");
        assertEquals(f.getDescription(), "delicious brazillian-style strogonoff");
        assertEquals(f.getRestaurant().getId(), createdRestaurant.getId());
    }


    @Test
    public void testCreateFoodWithRegularUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdRegularUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazillian-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 0);
    }


    @Test
    public void testCreateFoodWithImageSuccess() throws Exception {
        MockMultipartFile foodPhoto = new MockMultipartFile(
            "photo",
            "photo.jpg",
            MediaType.IMAGE_JPEG_VALUE,
            testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .file(foodPhoto)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazillian-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_CREATE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 1);
        Food f = foods.getFirst();
        assertEquals(f.getName(), "strogonoff");
        assertEquals(f.getDescription(), "delicious brazillian-style strogonoff");
        assertEquals(f.getPhotoPath(), createdRestaurant.getId().toString() + "/food/" + f.getId().toString() + ".jpg");
        assertEquals(f.getRestaurant().getId(), createdRestaurant.getId());
    }

    @Test
    public void tesstCreateUserWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .param("name", "strogonoff")
                        .param("description", "delicious brazillian-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 0);
    }


    @Test
    public void testCreateWithInvalidToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer alkjdsaldkjsaldkja")
                        .param("name", "strogonoff")
                        .param("description", "delicious brazillian-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 0);
    }


}
