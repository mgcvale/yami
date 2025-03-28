package com.yamiapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamiapp.config.TestConfig;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.model.Restaurant;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.repo.RestaurantRepository;
import com.yamiapp.repo.UserRepository;
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
import java.util.Optional;

import static com.yamiapp.util.TestUtils.createUserWithRole;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class RestaurantControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    private final UserDTO adminUser = new UserDTO("adminuser", "adminpassword123", "admin bio", "admin location", "admin@example.com");
    private final UserDTO regularUser = new UserDTO("regularuser", "userpassword123", "user bio", "user location", "user@example.com");
    private final UserDTO moderatorUser = new UserDTO("moderatoruser", "modpassword123", "mod bio", "mod location", "mod@example.com");

    private User createdAdminUser;
    private User createdRegularUser;
    private User createdModeratorUser;
    private byte[] testImageBytes;

    @BeforeAll
    public static void initialize() {
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(dotenvEntry -> System.setProperty(dotenvEntry.getKey(), dotenvEntry.getValue()));
    }

    @AfterEach
    public void cleanup() {
        restaurantRepository.deleteAll();
        userRepository.deleteAll();
    }

    @BeforeEach
    public void setup() throws Exception {
        createdAdminUser = createUserWithRole(mockMvc, objectMapper, userRepository, adminUser, Role.ADMIN);
        createdRegularUser = createUserWithRole(mockMvc, objectMapper, userRepository, regularUser, Role.USER);
        createdModeratorUser = createUserWithRole(mockMvc, objectMapper, userRepository, moderatorUser, Role.MODERATOR);

        try {
            testImageBytes = Files.readAllBytes(Paths.get("src/test/resources/test-image.png"));
        } catch (IOException e) {
            testImageBytes = new byte[1024]; // this will probably break some tests
        }

    }

    // RESTAURANT CREATION TESTS

    @Test
    public void testCreateRestaurantWithAdminSuccess() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", "Test Restaurant")
                        .param("description", "A test restaurant description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.RESTAURANT_CREATE_SUCCESS.getMessage()));

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(1, restaurants.size());
        assertEquals("Test Restaurant", restaurants.getFirst().getName());
        assertEquals("A test restaurant description", restaurants.getFirst().getDescription());
        assertNotNull(restaurants.getFirst().getPhotoPath());
    }

    @Test
    public void testCreateRestaurantWithModeratorSuccess() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", "Test Restaurant")
                        .param("description", "A test restaurant description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdModeratorUser.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.RESTAURANT_CREATE_SUCCESS.getMessage()));

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(1, restaurants.size());
    }

    @Test
    public void testCreateRestaurantWithRegularUserForbidden() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", "Test Restaurant")
                        .param("description", "A test restaurant description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage()));

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(0, restaurants.size());
    }

    @Test
    public void testCreateRestaurantWithInvalidToken() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(0, restaurants.size());

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", "Test Restaurant")
                        .param("description", "A test restaurant description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalidtoken123"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        restaurants = restaurantRepository.findAll();
        assertEquals(0, restaurants.size());
    }

    @Test
    public void testCreateRestaurantWithoutToken() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", "Test Restaurant")
                        .param("description", "A test restaurant description"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(0, restaurants.size());
    }

    @Test
    public void testCreateRestaurantWithDuplicateName() throws Exception {
        // Create first restaurant
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        final String name = "duplicate";
        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", name)
                        .param("description", "First restaurant")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isOk());

        // Try to create second restaurant with same name
        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", name)
                        .param("description", "Second restaurant with same name")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.CONFLICT_RESTAURANT_NAME.getMessage()));

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(1, restaurants.size());
    }

    @Test
    public void testCreateRestaurantWithMissingRequiredFields() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        // Missing name
        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("description", "A test restaurant description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isBadRequest());

        // Missing description
        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", "Test Restaurant")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isBadRequest());

        // Missing photo
        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .param("name", "Test Restaurant")
                        .param("description", "A test restaurant description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isBadRequest());

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(0, restaurants.size());
    }

    @Test
    public void testCreateRestaurantWithInvalidName() throws  Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", "do")
                        .param("Description", "Regular and valid description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorStrings.SHORT_RESTAURANT_NAME.getMessage()));

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(0, restaurants.size());
    }

    @Test
    public void testCreateRestaurantWithFileTooLarge() throws Exception {
        byte[] photoBytes = Files.readAllBytes(Paths.get("src/test/resources/large-image.jpg"));
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "large-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                photoBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photo)
                        .param("name", "Test Restaurant")
                        .param("description", "A test restaurant description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FILE_TOO_LARGE.getMessage()));

        List<Restaurant> restaurants = restaurantRepository.findAll();
        assertEquals(0, restaurants.size());
    }

    // RESTAURANT UPDATE TESTS

    @Test
    public void testUpdateRestaurantWithAdminSuccess() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "updated-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant/" + restaurant.getId().toString())
                        .file(photoFile)
                        .param("name", "Updated Restaurant")
                        .param("description", "Updated description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.RESTAURANT_UPDATE_SUCCESS.getMessage()));

        Optional<Restaurant> updatedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertTrue(updatedRestaurant.isPresent());
        assertEquals("Updated Restaurant", updatedRestaurant.get().getName());
        assertEquals("Updated description", updatedRestaurant.get().getDescription());
    }

    @Test
    public void testUpdateRestaurantWithModeratorSuccess() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "updated-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant/" + restaurant.getId().toString())
                        .file(photoFile)
                        .param("name", "Updated By Moderator")
                        .param("description", "Moderator description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdModeratorUser.getAccessToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        Optional<Restaurant> updatedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertTrue(updatedRestaurant.isPresent());
        assertEquals("Updated By Moderator", updatedRestaurant.get().getName());
    }

    @Test
    public void testUpdateRestaurantWithRegularUserForbidden() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "updated-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant/" + restaurant.getId().toString())
                        .file(photoFile)
                        .param("name", "Should Not Update")
                        .param("description", "Should not update description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdRegularUser.getAccessToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage()));

        Optional<Restaurant> notUpdatedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertTrue(notUpdatedRestaurant.isPresent());
        assertEquals("Test Restaurant", notUpdatedRestaurant.get().getName());
    }

    @Test
    public void testUpdateNonExistentRestaurant() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "updated-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant/99999")
                        .file(photoFile)
                        .param("name", "Non-existent Restaurant")
                        .param("description", "Should not update")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_RESTAURANT_ID.getMessage()));
    }

    @Test
    public void testPartialUpdateRestaurantOnlyName() throws Exception {
        Restaurant restaurant = createTestRestaurant();
        String originalDescription = restaurant.getDescription();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant/" + restaurant.getId().toString() )
                        .param("name", "Only Name Updated")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        Optional<Restaurant> updatedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertTrue(updatedRestaurant.isPresent());
        assertEquals("Only Name Updated", updatedRestaurant.get().getName());
        assertEquals(originalDescription, updatedRestaurant.get().getDescription());
    }

    @Test
    public void testPartialUpdateRestaurantOnlyDescription() throws Exception {
        Restaurant restaurant = createTestRestaurant();
        String originalName = restaurant.getName();

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant/" + restaurant.getId().toString())
                        .param("description", "Only description updated")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        Optional<Restaurant> updatedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertTrue(updatedRestaurant.isPresent());
        assertEquals(originalName, updatedRestaurant.get().getName());
        assertEquals("Only description updated", updatedRestaurant.get().getDescription());
    }

    @Test
    public void testUpdateRestaurantWithLargeFile() throws Exception {
        // First create a restaurant
        Restaurant restaurant = createTestRestaurant();

        byte[] photoBytes = Files.readAllBytes(Paths.get("src/test/resources/large-image.jpg"));
        MockMultipartFile photo = new MockMultipartFile(
                "photo",
                "large-image.jpg",
                MediaType.IMAGE_JPEG_VALUE,
                photoBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant/" + restaurant.getId().toString() )
                        .file(photo)
                        .param("name", "Updated Restaurant")
                        .param("description", "Updated description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .with(request -> {
                            request.setMethod("PATCH");
                            return request;
                        }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FILE_TOO_LARGE.getMessage()));

        Optional<Restaurant> updatedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertTrue(updatedRestaurant.isPresent(), "Restaurant should have the same ID after not being updated");
        assertEquals(updatedRestaurant.get().getName(), restaurant.getName());
        assertEquals(updatedRestaurant.get().getDescription(), restaurant.getDescription());
    }

    // RESTAURANT DELETION TESTS

    @Test
    public void testDeleteRestaurantWithAdminSuccess() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        UserLoginDTO loginInfo = new UserLoginDTO(adminUser.getUsername(), adminUser.getPassword());

        mockMvc.perform(delete("/restaurant/" + restaurant.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("username", loginInfo.getUsername())
                        .param("password", loginInfo.getPassword())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.RESTAURANT_DELETE_SUCCESS.getMessage()));

        Optional<Restaurant> deletedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertFalse(deletedRestaurant.isPresent(), "Restaurant should be deleted");
    }

    @Test
    public void testDeleteRestaurantWithModeratorSuccess() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        UserLoginDTO loginInfo = new UserLoginDTO(moderatorUser.getUsername(), moderatorUser.getPassword());

        mockMvc.perform(delete("/restaurant/" + restaurant.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdModeratorUser.getAccessToken())
                        .param("username", loginInfo.getUsername())
                        .param("password", loginInfo.getPassword())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"));

        Optional<Restaurant> deletedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertFalse(deletedRestaurant.isPresent(), "Restaurant should be deleted");
    }

    @Test
    public void testDeleteRestaurantWithRegularUserForbidden() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        UserLoginDTO loginInfo = new UserLoginDTO(regularUser.getUsername(), regularUser.getPassword());

        mockMvc.perform(delete("/restaurant/" + restaurant.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdRegularUser.getAccessToken())
                        .param("username", loginInfo.getUsername())
                        .param("password", loginInfo.getPassword())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage()));

        Optional<Restaurant> notDeletedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertTrue(notDeletedRestaurant.isPresent(), "Restaurant should not be deleted");
    }

    @Test
    public void testDeleteRestaurantWithInvalidCredentials() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        mockMvc.perform(delete("/restaurant/" + restaurant.getId().toString())
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("username", adminUser.getUsername())
                        .param("password", "adsdadasda")
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"));

        Optional<Restaurant> notDeletedRestaurant = restaurantRepository.findById(Math.toIntExact(restaurant.getId()));
        assertTrue(notDeletedRestaurant.isPresent(), "Restaurant should not be deleted");
    }

    @Test
    public void testDeleteNonExistentRestaurant() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(adminUser.getUsername(), adminUser.getPassword());

        mockMvc.perform(delete("/restaurant/999999")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("username", loginInfo.getUsername())
                        .param("password", loginInfo.getPassword())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_RESTAURANT_ID.getMessage()));
    }

    // RESTAURANT RETRIEVAL TESTS

    @Test
    public void testGetRestaurantById() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        mockMvc.perform(get("/restaurant/" + restaurant.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value(restaurant.getName()))
                .andExpect(jsonPath("$.description").value(restaurant.getDescription()))
                .andExpect(jsonPath("$.id").value(restaurant.getId()));
    }

    @Test
    public void testGetRestaurantWithInvalidId() throws Exception {
        mockMvc.perform(get("/restaurant/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_RESTAURANT_ID.getMessage()));
    }

    @Test
    public void testGetRestaurantImageById() throws Exception {
        Restaurant restaurant = createTestRestaurant();

        mockMvc.perform(get("/restaurant/" + restaurant.getId() + "/picture"))
                .andExpect(status().isOk())
                .andExpect(result -> {
                    assertEquals(MediaType.IMAGE_JPEG_VALUE, result.getResponse().getContentType());
                    assertTrue(result.getResponse().getContentAsByteArray().length > 0);
                });
    }

    @Test
    public void testGetRestaurantImageWithInvalidId() throws Exception {
        mockMvc.perform(get("/restaurant/99999/picture"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_RESTAURANT_ID.getMessage()));
    }

    // HELPER METHODS

    private Restaurant createTestRestaurant() throws Exception {
        MockMultipartFile photoFile = new MockMultipartFile(
                "photo",
                "test-image.png",
                MediaType.IMAGE_PNG_VALUE,
                testImageBytes
        );

        mockMvc.perform(MockMvcRequestBuilders.multipart("/restaurant")
                        .file(photoFile)
                        .param("name", "Test Restaurant")
                        .param("description", "A test restaurant description")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken()))
                .andExpect(status().isOk());

        List<Restaurant> restaurants = restaurantRepository.findAll();
        return restaurants.get(0);
    }
}