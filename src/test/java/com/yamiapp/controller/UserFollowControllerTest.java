package com.yamiapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamiapp.config.TestConfig;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.repo.UserFollowRepository;
import com.yamiapp.repo.UserRepository;
import com.yamiapp.service.UserFollowService;
import com.yamiapp.service.UserService;
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
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Import(TestConfig.class)
@ActiveProfiles("test")
public class UserFollowControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;


    private final UserDTO user1 = new UserDTO("user1", "user1pwd", "user1 bio", "user1 location", "user1@example.com");
    private final UserDTO user2 = new UserDTO("user2", "user2pwd", "user2 bio", "user2 location", "user2@example.com");
    private final UserDTO user3 = new UserDTO("user3", "user3pwd", "user3 bio", "user3 location", "user3@example.com");
    private final UserDTO user4 = new UserDTO("user4", "user4pwd", "user4 bio", "user4 location", "user4@example.com");
    private final UserDTO user5 = new UserDTO("user5", "user5pwd", "user5 bio", "user5 location", "user5@example.com");

    private User createdUser1, createdUser2, createdUser3, createdUser4, createdUser5;

    @Autowired
    private UserService userService;
    @Autowired
    private UserFollowService userFollowService;
    @Autowired
    private UserFollowRepository userFollowRepository;

    @BeforeAll
    public static void initialize() {
        Dotenv dotenv = Dotenv.load();
        dotenv.entries().forEach(dotenvEntry -> System.setProperty(dotenvEntry.getKey(), dotenvEntry.getValue()));
    }

    @AfterEach
    public void cleanup() {
        userRepository.deleteAll();
    }

    @BeforeEach
    public void setup() {
        createdUser1 = userService.createUser(user1);
        createdUser2 = userService.createUser(user2);
        createdUser3 = userService.createUser(user3);
        createdUser4 = userService.createUser(user4);
        createdUser5 = userService.createUser(user5);
    }

    @Test
    @Transactional
    public void createFollowSuccess() throws Exception {
        mockMvc.perform(post("/user/follow/" + createdUser2.getId())
                        .header("Authorization", "Bearer " + createdUser1.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOLLOW_CREATE_SUCCESS.getMessage()));

        User refreshedUser1 = userService.getById(createdUser1.getId());
        User refreshedUser2 = userService.getById(createdUser2.getId());

        assertTrue(refreshedUser1.getFollowing().contains(refreshedUser2));
        assertTrue(refreshedUser2.getFollowers().contains(refreshedUser1));
    }

    @Test
    @Transactional
    public void createFollowWithoutTokenError() throws Exception {
        mockMvc.perform(post("/user/follow/" + createdUser2.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

    }

    @Test
    @Transactional
    public void followOneselfError() throws Exception {
        mockMvc.perform(post("/user/follow/" + createdUser1.getId())
                        .header("Authorization", "Bearer " + createdUser1.getAccessToken()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.CANNOT_FOLLOW_ONESELF.getMessage()));

        User refreshedUser1 = userService.getById(createdUser1.getId());
        assertFalse(refreshedUser1.getFollowing().contains(refreshedUser1));
        assertFalse(refreshedUser1.getFollowers().contains(refreshedUser1));
    }

    @Test
    @Transactional
    public void followNonexistentUser() throws Exception {
        mockMvc.perform(post("/user/follow/89123")
                        .header("Authorization", "Bearer " + createdUser1.getAccessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USER_ID.getMessage()));

        User refreshedUser1 = userService.getById(createdUser1.getId());
        assertTrue(refreshedUser1.getFollowing().isEmpty());
    }

    @Test
    @Transactional
    public void refollowSameUserOK() throws Exception {
        createFollow(createdUser1, createdUser2);
        mockMvc.perform(post("/user/follow/" + createdUser2.getId())
                        .header("Authorization", "Bearer " + createdUser1.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOLLOW_CREATE_SUCCESS.getMessage()));

        User refreshedUser1 = userService.getById(createdUser1.getId());
        User refreshedUser2 = userService.getById(createdUser2.getId());

        assertTrue(refreshedUser1.getFollowing().contains(refreshedUser2));
        assertTrue(refreshedUser2.getFollowers().contains(refreshedUser1));
    }

    @Test
    @Transactional
    public void followFollowerSuccess() throws Exception {
        createFollow(createdUser1, createdUser2);
        mockMvc.perform(post("/user/follow/" + createdUser1.getId())
                        .header("Authorization", "Bearer " + createdUser2.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOLLOW_CREATE_SUCCESS.getMessage()));

        User refreshedUser1 = userService.getById(createdUser1.getId());
        User refreshedUser2 = userService.getById(createdUser2.getId());

        assertTrue(refreshedUser1.getFollowing().contains(refreshedUser2));
        assertTrue(refreshedUser2.getFollowers().contains(refreshedUser1));
        assertTrue(refreshedUser1.getFollowers().contains(refreshedUser2));
        assertTrue(refreshedUser2.getFollowing().contains(refreshedUser1));

    }

    @Test
    @Transactional
    public void deleteFollowSuccess() throws Exception {
        createFollow(createdUser1, createdUser2);

        mockMvc.perform(delete("/user/follow/" + createdUser2.getId())
                        .header("Authorization", "Bearer " + createdUser1.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOLLOW_DELETE_SUCCESS.getMessage()));

        User refreshedUser1 = userService.getById(createdUser1.getId());
        User refreshedUser2 = userService.getById(createdUser2.getId());

        assertFalse(refreshedUser1.getFollowing().contains(refreshedUser2));
        assertFalse(refreshedUser2.getFollowers().contains(refreshedUser1));
    }

    @Test
    @Transactional
    public void deleteNonexistingFollowOK() throws Exception {
        mockMvc.perform(delete("/user/follow/" + createdUser2.getId())
                        .header("Authorization", "Bearer " + createdUser1.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.FOLLOW_DELETE_SUCCESS.getMessage()));

        User refreshedUser1 = userService.getById(createdUser1.getId());
        User refreshedUser2 = userService.getById(createdUser2.getId());

        assertFalse(refreshedUser1.getFollowing().contains(refreshedUser2));
        assertFalse(refreshedUser2.getFollowers().contains(refreshedUser1));
    }


    @Transactional
    @Test
    public void deleteFollowBetweenNonexistentUsersError() throws Exception {
        mockMvc.perform(delete("/user/follow/8193219832")
                .header("Authorization", "Bearer " + createdUser1.getAccessToken()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USER_ID.getMessage()));
    }

    @Test
    @Transactional
    public void deleteFollowWithoutTokenError() throws Exception {
        createFollow(createdUser1, createdUser2);

        mockMvc.perform(delete("/user/follow/" + createdUser2.getId()))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        User refreshedUser1 = userService.getById(createdUser1.getId());
        User refreshedUser2 = userService.getById(createdUser2.getId());

        assertTrue(refreshedUser1.getFollowing().contains(refreshedUser2));
        assertTrue(refreshedUser2.getFollowers().contains(refreshedUser1));
    }

    @Test
    @Transactional
    public void getFollowingSuccess() throws Exception {
        createFollows(createdUser1, createdUser2, createdUser3, createdUser4, createdUser5); // user1 will follow everyone

        ResultActions matcher = mockMvc.perform(get("/user/follow/" + createdUser1.getId() + "/following")
                .header("Authorization", "Bearer " + createdUser1.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        List<User> createdUsers = List.of(createdUser2, createdUser3, createdUser4, createdUser5);
        List<String> expectedUsernames = createdUsers.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        List<Long> expectedIds = createdUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        List<String> expectedBios = createdUsers.stream()
                .map(User::getBio)
                .collect(Collectors.toList());

        matcher.andExpect(jsonPath("$[*].username").value(containsInAnyOrder(expectedUsernames.toArray())));
        matcher.andExpect(jsonPath("$[*].id").value(containsInAnyOrder(expectedIds.stream().map(Math::toIntExact).toList().toArray())));
        matcher.andExpect(jsonPath("$[*].bio").value(containsInAnyOrder(expectedBios.toArray())));
    }

    @Test
    @Transactional
    public void getFollowersSuccess() throws Exception {
        createFollowings(createdUser2, createdUser1, createdUser3, createdUser4, createdUser5);

        ResultActions matcher = mockMvc.perform(get("/user/follow/" + createdUser2.getId() + "/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(4));

        List<User> createdUsers = List.of(createdUser1, createdUser3, createdUser4, createdUser5);
        List<String> expectedUsernames = createdUsers.stream()
                .map(User::getUsername)
                .collect(Collectors.toList());

        List<Long> expectedIds = createdUsers.stream()
                .map(User::getId)
                .collect(Collectors.toList());

        List<String> expectedBios = createdUsers.stream()
                .map(User::getBio)
                .collect(Collectors.toList());

        matcher.andExpect(jsonPath("$[*].username").value(containsInAnyOrder(expectedUsernames.toArray())));
        matcher.andExpect(jsonPath("$[*].id").value(containsInAnyOrder(expectedIds.stream().map(Math::toIntExact).toList().toArray())));
        matcher.andExpect(jsonPath("$[*].bio").value(containsInAnyOrder(expectedBios.toArray())));
    }

    @Test
    public void getFollowingOfNonexistentUserError() throws Exception {
        mockMvc.perform(get("/user/follow/123131/followers"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USER_ID.getMessage()));
    }
    @Test
    public void getFollowersOfNonexistentUserError() throws Exception {
        mockMvc.perform(get("/user/follow/13213/following"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USER_ID.getMessage()));
    }

    @Test
    @Transactional
    public void getFollowersShouldntReturnSensitiveData() throws Exception {
        createFollow(createdUser1, createdUser2);
        mockMvc.perform(get("/user/follow/" + Math.toIntExact(createdUser1.getId()) + "/followers"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    @Transactional
    public void getFollowingShouldntReturnSensitiveData() throws Exception {
        createFollow(createdUser1, createdUser2);
        mockMvc.perform(get("/user/follow/" + Math.toIntExact(createdUser1.getId()) + "/following"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].accessToken").doesNotExist())
                .andExpect(jsonPath("$[0].passwordHash").doesNotExist())
                .andExpect(jsonPath("$[0].email").doesNotExist());
    }

    @Test
    @Transactional
    public void IsFollowingTrueSuccess() throws Exception {
        createFollow(createdUser1, createdUser2);

        mockMvc.perform(get("/user/follow/" + createdUser1.getId() + "/following/" + createdUser2.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(true));
    }

    @Test
    @Transactional
    public void IsFollowingFalseSuccess() throws Exception {
        createFollow(createdUser1, createdUser2);

        mockMvc.perform(get("/user/follow/" + createdUser2.getId() + "/following/" + createdUser1.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.following").value(false));
    }

    @Test
    public void isFollowingNonExistentUserError() throws  Exception {
        mockMvc.perform(get("/user/follow/123/following/124"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USER_ID.getMessage()));
    }

    public void createFollow(User user1, User user2) {
        userFollowService.follow(user1.getAccessToken(), user2.getId());

        User refreshedUser1 = userService.getById(createdUser1.getId());
        User refreshedUser2 = userService.getById(createdUser2.getId());

        assertTrue(refreshedUser1.getFollowing().contains(refreshedUser2));
        assertTrue(refreshedUser2.getFollowers().contains(refreshedUser1));
    }

    public void createFollows(User follower, User ...followings) {
        for (User following : followings) {
            createFollow(follower, following);
        }
    }

    public void createFollowings(User following, User ...followers) {
        for (User follower : followers) {
            createFollow(follower, following);
        }
    }

}
