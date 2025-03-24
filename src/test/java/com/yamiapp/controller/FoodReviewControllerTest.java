package com.yamiapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamiapp.config.TestConfig;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.mock.FakeBackblazeService;
import com.yamiapp.model.*;
import com.yamiapp.model.dto.FoodDTO;
import com.yamiapp.model.dto.FoodResponseDTO;
import com.yamiapp.model.dto.FoodReviewDTO;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.repo.FoodRepository;
import com.yamiapp.repo.FoodReviewRepository;
import com.yamiapp.repo.RestaurantRepository;
import com.yamiapp.repo.UserRepository;
import com.yamiapp.service.FoodReviewService;
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
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static com.yamiapp.util.TestUtils.createUserWithRole;


@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
public class FoodReviewControllerTest {
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
    private FoodReviewDTO foodReviewDTO;

    private User createdAdminUser;
    private User createdRegularUser;
    private User createdModeratorUser;
    private Restaurant createdRestaurant;
    private Food createdFood;
    private byte[] testImageBytes;
    @Autowired
    private FoodReviewRepository foodReviewRepository;
    @Autowired
    private FoodReviewService foodReviewService;

    @BeforeAll
    public static void initialize() {
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(dotenvEntry -> System.setProperty(dotenvEntry.getKey(), dotenvEntry.getValue()));
    }

    @BeforeEach
    public void setup() throws Exception {
        // create users
        userRepository.deleteAll();
        createdAdminUser = createUserWithRole(mockMvc, objectMapper, userRepository, adminUser, Role.ADMIN);
        createdRegularUser = createUserWithRole(mockMvc, objectMapper, userRepository, regularUser, Role.USER);
        createdModeratorUser = createUserWithRole(mockMvc, objectMapper, userRepository, moderatorUser, Role.MODERATOR);
        backblazeService = new FakeBackblazeService();


        // create restaurant
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

        // create food
        mockMvc.perform(MockMvcRequestBuilders.multipart("/food")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdAdminUser.getAccessToken())
                        .param("name", "strogonoff")
                        .param("description", "delicious brazilian-style strogonoff")
                        .param("restaurantId", createdRestaurant.getId().toString()))
                .andExpect(status().isOk());

        List<Food> foods = foodRepository.findAll();
        System.out.println(foods);
        assertEquals(1, foods.size());
        createdFood = foods.getFirst();

        foodReviewDTO = FoodReviewDTO.builder().review("Default food review").rating(11).build();
    }

    @AfterEach
    public void cleanup() throws Exception {
        foodReviewRepository.deleteAll();
        restaurantRepository.deleteAll();
        foodRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    public void createFoodReviewWithRegularUserSucces() throws Exception {
        createDefaultFoodReview(createdRegularUser);
    }

    @Test
    public void createFoodReviewWithAdminUserSucces() throws Exception {
        createDefaultFoodReview(createdAdminUser);
    }

    @Test
    public void createFoodReviewWithoutToken() throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));
    }

    @Test
    public void createFoodReviewWithInvalidFoodId() throws Exception {
        long invalidFoodId = -1L;
        mockMvc.perform(post("/food/review/" + invalidFoodId)
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO))
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isNotFound());
    }

    @Test
    public void createFoodReviewWithInvalidSmallReview() throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO.withReview("a")))
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.BAD_FOOD_REVIEW_LENGTH.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void createFoodReviewWithInvalidBigReview() throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO.withReview(
                                new String(new char[513]).replace('\0', 'A')
                        )))
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.BAD_FOOD_REVIEW_LENGTH.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void createFoodReviewWithoutFood() throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO.withReview(null)))
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.EMPTY_FIELDS.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void createFoodReviewWithMissingRating() throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO.withRating(null)))
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.EMPTY_FIELDS.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void createFoodReviewWithInvalidLowRating() throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO.withRating(-1)))
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.BAD_FOOD_REVIEW_RATING.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void createFoodReviewWithInvalidHighRating() throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO.withRating(21)))
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.BAD_FOOD_REVIEW_RATING.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void createFoodReviewWithInvalidToken() throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO))
                        .header("Authorization", "Bearer llkasdj"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void createFoodReviewWithMinRating() throws Exception {
        FoodReviewDTO dto = foodReviewDTO.withRating(0);
        createAndVerifyReview(dto, createdRegularUser);
    }

    @Test
    public void createFoodReviewWithMaxRating() throws Exception {
        FoodReviewDTO dto = foodReviewDTO.withRating(20);
        createAndVerifyReview(dto, createdRegularUser);
    }

    private void createAndVerifyReview(FoodReviewDTO dto, User user) throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(dto))
                        .header("Authorization", "Bearer " + user.getAccessToken()))
                .andExpect(status().isOk());

        FoodReview review = foodReviewRepository.findAll().getFirst();
        assertEquals(dto.getRating(), review.getRating());
    }

    @Test
    public void createDuplicateFoodReview() throws Exception {
        createDefaultFoodReview(createdRegularUser);
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(foodReviewDTO))
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(ErrorStrings.CONFLICT.getMessage()));
    }

    @Test
    public void deleteFoodReviewSuccess() throws Exception {
        FoodReview review = createDefaultFoodReview(createdRegularUser);
        mockMvc.perform(delete("/food/review/" + review.getId())
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isOk());

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void deleteFoodReviewWithoutBeingFoodOwner() throws Exception {
        FoodReview review = createDefaultFoodReview(createdModeratorUser);
        mockMvc.perform(delete("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.NOT_OWNER_OF_FOOD_REVIEW.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
    }

    @Test
    public void deleteFoodReviewWithInvalidToken() throws Exception {
        FoodReview review = createDefaultFoodReview(createdModeratorUser);
        mockMvc.perform(delete("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .header("Authorization", "Bearer alksdjsaslkdjsaldksaj"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
    }

    @Test
    public void deleteFoodReviewWithoutToken() throws Exception {
        FoodReview review = createDefaultFoodReview(createdModeratorUser);
        mockMvc.perform(delete("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
    }

    @Test
    public void updateFoodReviewSuccess() throws Exception {
        FoodReview review = createDefaultFoodReview(createdRegularUser);
        FoodReviewDTO updateDto = FoodReviewDTO.builder().review("NewReview").rating(2).build();
        mockMvc.perform(patch("/food/review/" + review.getId())
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review").value("NewReview"));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
        FoodReview editedReview = foodReviews.getFirst();
        assertEquals("NewReview", editedReview.getReview());
        assertEquals(2, editedReview.getRating());
    }

    @Test
    public void updateFoodReviewWithOnlyOneFieldSuccess() throws Exception {
        FoodReview review = createDefaultFoodReview(createdRegularUser);
        FoodReviewDTO updateDto = FoodReviewDTO.builder().review("NewReview").build();
        mockMvc.perform(patch("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review").value("NewReview"));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
        FoodReview editedReview = foodReviews.getFirst();
        assertEquals("NewReview", editedReview.getReview());
    }

    @Test
    public void updateFoodReviewWithInvalidTokenError() throws Exception {
        FoodReview review = createDefaultFoodReview(createdAdminUser);
        FoodReviewDTO updateDto = FoodReviewDTO.builder().review("NewReview").rating(2).build();
        mockMvc.perform(patch("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.NOT_OWNER_OF_FOOD_REVIEW.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
        FoodReview editedReview = foodReviews.getFirst();
        assertEquals(review.getReview(), editedReview.getReview());
    }

    @Test
    public void updateFoodReviewWithoutTokenError() throws Exception {
        FoodReview review = createDefaultFoodReview(createdAdminUser);
        FoodReviewDTO updateDto = FoodReviewDTO.builder().review("NewReview").rating(2).build();
        mockMvc.perform(patch("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .content(objectMapper.writeValueAsString(updateDto)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
        FoodReview editedReview = foodReviews.getFirst();
        assertEquals(review.getReview(), editedReview.getReview());
    }

    @Test
    public void updateFoodReviewWithRatingTooLarge() throws Exception {
        FoodReview review = createDefaultFoodReview(createdRegularUser);
        FoodReviewDTO invalidUpdateDto = FoodReviewDTO.builder().rating(21).build();

        mockMvc.perform(patch("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                        .content(objectMapper.writeValueAsString(invalidUpdateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorStrings.BAD_FOOD_REVIEW_RATING.getMessage()));
    }

    @Test
    public void updateFoodReviewWithRatingTooSmall() throws Exception {
        FoodReview review = createDefaultFoodReview(createdRegularUser);
        FoodReviewDTO invalidUpdateDto = FoodReviewDTO.builder().rating(-1).build();

        mockMvc.perform(patch("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                        .content(objectMapper.writeValueAsString(invalidUpdateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorStrings.BAD_FOOD_REVIEW_RATING.getMessage()));
    }

    @Test
    public void updateFoodReviewWithReviewTooSmall() throws Exception {
        FoodReview review = createDefaultFoodReview(createdRegularUser);
        FoodReviewDTO invalidUpdateDto = FoodReviewDTO.builder().review("a").build();

        mockMvc.perform(patch("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                        .content(objectMapper.writeValueAsString(invalidUpdateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorStrings.BAD_FOOD_REVIEW_LENGTH.getMessage()));
    }

    @Test
    public void updateFoodReviewWithReviewTooLarge() throws Exception {
        FoodReview review = createDefaultFoodReview(createdRegularUser);
        FoodReviewDTO invalidUpdateDto = FoodReviewDTO.builder().review(new String(new char[513]).replace('\0', 'a')).build();

        mockMvc.perform(patch("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType())
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                        .content(objectMapper.writeValueAsString(invalidUpdateDto)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(ErrorStrings.BAD_FOOD_REVIEW_LENGTH.getMessage()));
    }


    @Test
    public void deteleFoodReviewSuccess() throws Exception {
        FoodReview review = createDefaultFoodReview(createdRegularUser);
        mockMvc.perform(delete("/food/review/" + review.getId())
                .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                .contentType(ContentType.APPLICATION_JSON.getMimeType()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOOD_REVIEW_DELETE_SUCCESS.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(0, foodReviews.size());
    }

    @Test
    public void deleteFoodReviewWithInvalidTokenError() throws Exception {
        FoodReview review = createDefaultFoodReview(createdAdminUser);
        mockMvc.perform(delete("/food/review/" + review.getId())
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType()))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.NOT_OWNER_OF_FOOD_REVIEW.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
    }

    @Test
    public void deleteNonExistentFoodReviewError() throws Exception {
        FoodReview review = createDefaultFoodReview(createdAdminUser);
        mockMvc.perform(delete("/food/review/" + 39128)
                        .header("Authorization", "Bearer " + createdRegularUser.getAccessToken())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_FOOD_REVIEW_ID.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
    }

    @Test
    public void deleteFoodReviewWithoutTokenError() throws Exception {
        FoodReview review = createDefaultFoodReview(createdAdminUser);
        mockMvc.perform(delete("/food/review/" + review.getId())
                        .contentType(ContentType.APPLICATION_JSON.getMimeType()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertEquals(1, foodReviews.size());
    }

    @Test
    public void testGetFoodReviewByIdSuccess() throws Exception {
        FoodReview review = createDefaultFoodReview(createdAdminUser);
        mockMvc.perform(get("/food/review/" + review.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review").value(review.getReview()))
                .andExpect(jsonPath("$.rating").value(review.getRating()))
                .andExpect(jsonPath("$.foodName").value(review.getFood().getName()))
                .andExpect(jsonPath("$.username").value(review.getUser().getUsername()))
                .andExpect(jsonPath("$.userId").value(review.getUser().getId()))
                .andExpect(jsonPath("$.foodId").value(review.getFood().getId()));
    }

    @Test
    public void testGetNonexistentFoodReviewError() throws Exception {
        mockMvc.perform(get("/food/review/123"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_FOOD_ID.getMessage()));
    }


    // tests involving gathering lots of reviews with paging
    @Test
    public void getFoodReviewsSuccess() throws Exception {
        // Create 3 reviews for the food
        createDefaultFoodReview(createdRegularUser);
        createDefaultFoodReview(createdAdminUser);
        createDefaultFoodReview(createdModeratorUser);

        mockMvc.perform(get("/food/" + createdFood.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(3));
    }

    @Test
    public void getFoodReviewsWithPagination() throws Exception {
        // Create 5 reviews
        createFoodReviewBatchFromOneFood(createdFood, 5);

        // First page of 2
        mockMvc.perform(get("/food/" + createdFood.getId() + "/reviews?offset=0&count=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalPages").value(3));

        // Second page
        mockMvc.perform(get("/food/" + createdFood.getId() + "/reviews?offset=1&count=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    public void getFoodReviewsWithKeywordFilter() throws Exception {
        FoodReview review1 = createDefaultFoodReview(createdRegularUser, "Great burger");
        FoodReview review2 = createDefaultFoodReview(createdAdminUser, "Average fries");

        mockMvc.perform(get("/food/" + createdFood.getId() + "/reviews?keyword=burger"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(review1.getId()));
    }

    @Test
    public void getUserReviewsSuccess() throws Exception {
        // Create 2 reviews from regular user
        createFoodReviewBatchFromOneUser(createdRegularUser, 2);
        // And only 1 for admin
        createFoodReviewBatchFromOneUser(createdAdminUser, 1);

        mockMvc.perform(get("/user/" + createdRegularUser.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2));
    }

    @Test
    public void getUserReviewsWithPagination() throws Exception {
        // Create 4 reviews
        createFoodReviewBatchFromOneUser(createdRegularUser, 5);

        mockMvc.perform(get("/user/" + createdRegularUser.getId() + "/reviews?count=2"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(2))
                .andExpect(jsonPath("$.totalElements").value(5));
    }

    @Test
    public void getUserReviewsWithKeywordFilter() throws Exception {
        FoodReview review1 = createDefaultFoodReview(createdRegularUser, "Best pizza ever");
        createFoodReviewBatchFromOneUser(createdAdminUser, 3);

        mockMvc.perform(get("/user/" + createdRegularUser.getId() + "/reviews?keyword=pizza"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1))
                .andExpect(jsonPath("$.content[0].id").value(review1.getId()));
    }

    @Test
    public void getUserReviewsForUserWithNoReviews() throws Exception {
        mockMvc.perform(get("/user/" + createdModeratorUser.getId() + "/reviews"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(0));
    }

    @Test
    public void getUserReviewsForNonExistentUser() throws Exception {
        mockMvc.perform(get("/user/999/reviews"))
                .andExpect(status().isNotFound());
    }

    @Test
    public void getFoodReviewsForNonExistentFood() throws Exception {
        mockMvc.perform(get("/food/999/reviews"))
                .andExpect(status().isNotFound());
    }


    public FoodReview createDefaultFoodReview(User u) throws Exception {
        return createDefaultFoodReview(u, foodReviewDTO.getReview());
    }

    public void createFoodReviewBatchFromOneUser(User u, int count) throws Exception {

        for (int i = 0; i < count; i++) {
            // we need to create a new food for each review
            FoodDTO foodDTO = FoodDTO.builder()
                            .name("cool food " + i + u.getId())
                            .description("food description" + i)
                            .restaurantId(Math.toIntExact(createdRestaurant.getId()))
                    .build();
            MvcResult result = mockMvc.perform(multipart("/food")
                            .contentType(ContentType.APPLICATION_JSON.getMimeType())
                            .header("Authorization", "Bearer " + createdAdminUser.getAccessToken())
                            .param("name", foodDTO.getName())
                            .param("description", foodDTO.getDescription())
                            .param("restaurantId", foodDTO.getRestaurantId().toString()))
                    .andExpect(status().isOk())
                    .andReturn();
            Long foodId = objectMapper.readTree(result.getResponse().getContentAsString()).path("id").asLong();


            // now we create the review for that food
            mockMvc.perform(post("/food/review/" + foodId)
                            .contentType(ContentType.APPLICATION_JSON.getMimeType())
                            .content(objectMapper.writeValueAsString(foodReviewDTO))
                            .header("Authorization", "Bearer " + u.getAccessToken()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.review").value(foodReviewDTO.getReview()))
                    .andExpect(jsonPath("$.rating").value(foodReviewDTO.getRating()))
                    .andExpect(jsonPath("$.foodId").value(foodId))
                    .andExpect(jsonPath("$.foodName").value(foodDTO.getName()))
                    .andExpect(jsonPath("$.userId").value(u.getId()))
                    .andExpect(jsonPath("$.username").value(u.getUsername()))
                    .andExpect(jsonPath("$.id").exists());
        }
    }

    public void createFoodReviewBatchFromOneFood(Food f, int count) throws Exception {

        for (int i = 0; i < count; i++) {
            // we need to create a new user for each review
            UserDTO userDTO = new UserDTO("username" + i + f.getId(), "pwd123123", "bio", "location", "email" + i + "@gmail.com");
            MvcResult result = mockMvc.perform(multipart("/user")
                            .contentType(ContentType.APPLICATION_JSON.getMimeType())
                            .header("Authorization", "Bearer " + createdAdminUser.getAccessToken())
                            .content(objectMapper.writeValueAsString(userDTO)))
                    .andExpect(status().isOk())
                    .andReturn();
            String userToken = objectMapper.readTree(result.getResponse().getContentAsString()).path("accessToken").asText();

            // now we create the review for that food
            mockMvc.perform(post("/food/review/" + f.getId())
                            .contentType(ContentType.APPLICATION_JSON.getMimeType())
                            .content(objectMapper.writeValueAsString(foodReviewDTO))
                            .header("Authorization", "Bearer " + userToken))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.review").value(foodReviewDTO.getReview()))
                    .andExpect(jsonPath("$.rating").value(foodReviewDTO.getRating()))
                    .andExpect(jsonPath("$.foodId").value(createdFood.getId()))
                    .andExpect(jsonPath("$.foodName").value(f.getName()))
                    .andExpect(jsonPath("$.id").exists());
        }
    }

    public FoodReview createDefaultFoodReview(User u, String differentReview) throws Exception {
        mockMvc.perform(post("/food/review/" + createdFood.getId())
                .contentType(ContentType.APPLICATION_JSON.getMimeType())
                .content(objectMapper.writeValueAsString(foodReviewDTO.withReview(differentReview)))
                .header("Authorization", "Bearer " + u.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.review").value(differentReview))
                .andExpect(jsonPath("$.rating").value(foodReviewDTO.getRating()))
                .andExpect(jsonPath("$.foodId").value(createdFood.getId()))
                .andExpect(jsonPath("$.foodName").value(createdFood.getName()))
                .andExpect(jsonPath("$.userId").value(u.getId()))
                .andExpect(jsonPath("$.username").value(u.getUsername()))
                .andExpect(jsonPath("$.id").exists());

        List<FoodReview> foodReviews = foodReviewRepository.findAll();
        assertFalse(foodReviews.isEmpty());
        FoodReview r = foodReviews.getFirst();

        return r;
    }
}