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
import org.apache.http.entity.ContentType;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;

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

    private final UserDTO adminUser = new UserDTO("adminuser", "adminpassword123", "admin bio", "admin location", "admin@example.com");
    private final UserDTO regularUser = new UserDTO("regularuser", "userpassword123", "user bio", "user location", "user@example.com");
    private final UserDTO moderatorUser = new UserDTO("moderatoruser", "modpassword123", "mod bio", "mod location", "mod@example.com");

    private User createdAdminUser;
    private User createdRegularUser;
    private User createdModeratorUser;
    private Restaurant createdRestaurant;
    private byte[] testImageBytes;

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
                        .param("description", "delicious brazilian" +
                                "-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("strogonoff"))
                .andExpect(jsonPath("$.restaurantName").value(createdRestaurant.getName()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 1);
        Food f = foods.getFirst();
        assertEquals(f.getName(), "strogonoff");
        assertEquals(f.getDescription(), "delicious brazilian" +
                "-style strogonoff");
        assertEquals(f.getRestaurant().getId(), createdRestaurant.getId());
    }


    @Test
    public void testCreateFoodWithModeratorSuccess() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdModeratorUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazilian" +
                                "-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("strogonoff"))
                .andExpect(jsonPath("$.restaurantName").value(createdRestaurant.getName()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 1);
        Food f = foods.getFirst();
        assertEquals(f.getName(), "strogonoff");
        assertEquals(f.getDescription(), "delicious brazilian" +
                "-style strogonoff");
        assertEquals(f.getRestaurant().getId(), createdRestaurant.getId());
    }


    @Test
    public void testCreateFoodWithRegularUser() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdRegularUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazilian" +
                                "-style strogonoff")
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
                        .param("description", "delicious brazilian" +
                                "-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("strogonoff"))
                .andExpect(jsonPath("$.restaurantName").value(createdRestaurant.getName()));
        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 1);
        Food f = foods.getFirst();
        assertEquals(f.getName(), "strogonoff");
        assertEquals(f.getDescription(), "delicious brazilian" +
                "-style strogonoff");
        assertEquals(f.getPhotoPath(), createdRestaurant.getId().toString() + "/food/" + f.getId().toString() + ".jpg");
        assertEquals(f.getRestaurant().getId(), createdRestaurant.getId());
    }

    @Test
    public void tesstCreateUserWithoutToken() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .param("name", "strogonoff")
                        .param("description", "delicious brazilian" +
                                "-style strogonoff")
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
                        .param("description", "delicious brazilian" +
                                "-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 0);
    }


    @Test
    public void testCreaeFoodWithConflictingName() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazilian" +
                                "-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("strogonoff"))
                .andExpect(jsonPath("$.restaurantName").value(createdRestaurant.getName()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 1);
        Food f = foods.getFirst();
        assertEquals(f.getName(), "strogonoff");
        assertEquals(f.getDescription(), "delicious brazilian" +
                "-style strogonoff");
        assertEquals(f.getRestaurant().getId(), createdRestaurant.getId());


        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "different brazilian" +
                                "-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.CONFLICT_FOOD_NAME.getMessage()));

        foods = foodRepository.findAll();
        assertEquals(foods.size(), 1);
        f = foods.getFirst();
        assertEquals(f.getDescription(), "delicious brazilian" +
                "-style strogonoff");
    }

    @Test
    public void testCreateFoodWithFileTooLarge() throws Exception {
        byte[] photoBytes = Files.readAllBytes(Paths.get("src/test/resources/large-image.jpg"));
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "large-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                photoBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .file(photo)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazilian" +
                                "-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FILE_TOO_LARGE.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(foods.size(), 0);
    }

    @Test
    public void testUpdateFoodWithAdminSuccess() throws Exception {
        Food createdFood = createTestFood();

        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food/" + createdFood.getId().toString())
                    .file(photo)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                    .param("name", "newName")
                    .param("description", "newDescription")
                    .with(request -> { request.setMethod("PATCH"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_UPDATE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
        Food editedFood = foods.getFirst();
        assertEquals("newName", editedFood.getName());
        assertEquals("newDescription", editedFood.getDescription());
    }

    @Test
    public void testUpdateFoodWithModeratorSuccess() throws Exception {
        Food createdFood = createTestFood();

        MockMultipartFile newFoodPhoto = new MockMultipartFile(
                "photo",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food/" + createdFood.getId().toString())
                        .file(newFoodPhoto)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdModeratorUser.getAccessToken())
                        .param("name", "newName")
                        .param("description", "newDescription")
                        .with(request -> { request.setMethod("PATCH"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_UPDATE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
        Food editedFood = foods.getFirst();
        assertEquals("newName", editedFood.getName());
        assertEquals("newDescription", editedFood.getDescription());
    }


    @Test
    public void testUpdateFoodWithRegularUserError() throws Exception {
        Food createdFood = createTestFood();

        MockMultipartFile newFoodPhoto = new MockMultipartFile(
                "photo",
                "photo.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food/" + createdFood.getId().toString())
                        .file(newFoodPhoto)
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdRegularUser.getAccessToken())
                        .param("name", "newName")
                        .param("description", "newDescription")
                        .with(request -> { request.setMethod("PATCH"); return request; }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
        Food editedFood = foods.getFirst();
        assertEquals(createdFood.getName(), editedFood.getName());
        assertEquals(createdFood.getDescription(), editedFood.getDescription());
    }

    @Test
    public void testUpdateFoodWithMissingFieldsSucecss() throws Exception {
        Food createdFood = createTestFood();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food/" + createdFood.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "newName")
                        .with(request -> { request.setMethod("PATCH"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_UPDATE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
        Food editedFood = foods.getFirst();
        assertEquals("newName", editedFood.getName());
    }

    @Test
    public void testUpdateFoodWithLargeFileError() throws Exception {
        Food createdFood = createTestFood();
        byte[] photoBytes = Files.readAllBytes(Paths.get("src/test/resources/large-image.jpg"));
        MockMultipartFile largePhoto = new MockMultipartFile(
                "photo",
                "large-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                photoBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food/" + createdFood.getId().toString())
                        .file(largePhoto)
                        .param("name", "New Name")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .with(request -> { request.setMethod("PATCH"); return request; }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FILE_TOO_LARGE.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
        Food editedFood = foods.getFirst();
        assertEquals(editedFood.getName(), createdFood.getName());
    }

    @Test
    public void testUpdateFoodWithoutIdError() throws Exception {
        Food createdFood = createTestFood();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "newName")
                        .with(request -> { request.setMethod("PATCH"); return request; }))
                .andExpect(status().isMethodNotAllowed());

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
        Food editedFood = foods.getFirst();
        assertEquals(createdFood.getName(), editedFood.getName());
    }

    @Test
    public void testUpdateFoodWithInvalidIdError() throws Exception {
        Food createdFood = createTestFood();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food/903182")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "newName")
                        .with(request -> { request.setMethod("PATCH"); return request; }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_FOOD_ID.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
        Food editedFood = foods.getFirst();
        assertEquals(createdFood.getName(), editedFood.getName());
    }

    @Test
    public void testDeleteFoodWithAdminSuccess() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/" + toBeDeleted.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("username", adminUser.getUsername())
                        .param("password", adminUser.getPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_DELETE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(0, foods.size());
    }

    @Test
    public void testDeleteFoodWithModeratorSuccess() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/" + toBeDeleted.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdModeratorUser.getAccessToken())
                        .param("username", moderatorUser.getUsername())
                        .param("password", moderatorUser.getPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_DELETE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(0, foods.size());
    }

    @Test
    public void testDeleteFoodWithAdminEmail() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/" + toBeDeleted.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("email", adminUser.getEmail())
                        .param("password", adminUser.getPassword()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_DELETE_SUCCESS.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(0, foods.size());
    }

    @Test
    public void testDeleteFoodWithoutPasswordError() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/" + toBeDeleted.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("username", adminUser.getUsername()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
    }

    @Test
    public void testDeleteFoodWithInvalidPasswordError() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/" + toBeDeleted.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("username", adminUser.getUsername())
                        .param("password", "wrongpassword012983"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
    }

    @Test
    public void testDeleteFoodWithInvalidtokenError() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/" + toBeDeleted.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer 123")
                        .param("username", adminUser.getUsername())
                        .param("password", adminUser.getPassword()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
    }

    @Test
    public void testDeleteFoodWithoutTokenError() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/" + toBeDeleted.getId().toString())
                        .param("username", adminUser.getUsername())
                        .param("password", adminUser.getPassword()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
    }

    @Test
    public void testDeleteFoodWithRegularUserError() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/" + toBeDeleted.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdRegularUser.getAccessToken())
                        .param("username", regularUser.getUsername())
                        .param("password", regularUser.getPassword()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
    }

    @Test
    public void testDeletFoodWithoutFoodIdError() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("username", adminUser.getUsername())
                        .param("password", adminUser.getPassword()))
                .andExpect(status().isMethodNotAllowed());

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
    }

    @Test
    public void testDeleteFoodWithInvalidIdError() throws Exception {
        Food toBeDeleted = createTestFood();

        mockMvc.perform(delete("/food/120938")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("username", adminUser.getUsername())
                        .param("password", adminUser.getPassword()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_FOOD_ID.getMessage()));

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
    }

    @Test
    public void testGetFoodSuccess() throws Exception {
        Food createdFood = createTestFood();
        mockMvc.perform(get("/food/" + createdFood.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(createdFood.getName()))
                .andExpect(jsonPath("$.description").value(createdFood.getDescription()))
                .andExpect(jsonPath("$.restaurantId").value(createdFood.getRestaurant().getId().toString()));
    }

    @Test
    public void testGetFoodImageSuccess() throws Exception {
        Food createdFood = createTestFood();
        mockMvc.perform(get("/food/" + createdFood.getId() + "/picture"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(ContentType.IMAGE_JPEG.getMimeType()));

    }

    @Test
    public void testGetFoodWrongIdError() throws Exception {
        Food createdFood = createTestFood();
        mockMvc.perform(get("/food/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_FOOD_ID.getMessage()));

    }

    @Test
    public void testGetFoodWithoutIdError() throws Exception {
        Food createdFood = createTestFood();
        mockMvc.perform(get("/food"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.METHOD_NOT_ALLOWED.getMessage()));

    }

    public void testGetFoodImageWithWrongIdError() throws Exception {
        Food createdFood = createTestFood();
        mockMvc.perform(get("/food/102930219/picture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_FOOD_ID.getMessage()));
    }



    private Food createTestFood() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                    .file(photoFile)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                    .param("name", "strogonoff")
                    .param("description", "delicious brazilian-style strogonoff")
                    .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk());

        List<Food> foods = foodRepository.findAll();
        assertEquals(1, foods.size());
        return foods.getFirst();
    }
}
