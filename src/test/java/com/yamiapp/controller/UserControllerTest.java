package com.yamiapp.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.model.dto.UserLoginDTO;
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
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Transactional
@SpringBootTest
@ActiveProfiles("test")
@AutoConfigureMockMvc
public class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserDTO defaultUser = new UserDTO("testuser", "password123", "bio", "location", "test@example.com");
    private final UserDTO differentUser = new UserDTO("differentuser", "differentpassword", "differentbio", "differentlocation", "diferentemail@example.com");
    private User createdUser;

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
    public void setup() throws Exception {
        String json = objectMapper.writeValueAsString(defaultUser);
        mockMvc.perform(post("/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(defaultUser.getUsername()))
                .andExpect(jsonPath("$.email").value(defaultUser.getEmail()))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.id").exists());

        Optional<User> savedUser = userRepository.findByUsername(defaultUser.getUsername());
        assertTrue(savedUser.isPresent());
        createdUser = savedUser.get();
    }



    // USER CREATION
    @Test
    public void testCreateUserSuccess() throws Exception {
        assertEquals("testuser", createdUser.getUsername());
        assertEquals("test@example.com", createdUser.getEmail());
        assertTrue(encoder.matches("password123", createdUser.getPasswordHash()));
        assertNotNull(createdUser.getAccessToken());
        assertEquals(Role.USER, createdUser.getRole());
    }

    @Test
    public void testCreateUserUsernameConflict() throws Exception {
        String differentemail = "differentemail@email.com";
        UserDTO user2 = defaultUser.copy().withEmail(differentemail);
        String json2 = objectMapper.writeValueAsString(user2);

        mockMvc.perform(post("/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(json2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.CONFLICT_USERNAME.getMessage()));

        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
        assertNotEquals(users.getFirst().getEmail(), differentemail);
    }

    @Test
    public void testCreateUserEmailConflict() throws Exception {
        String differentUsername = "newusername(different)";
        UserDTO user2 = defaultUser.copy().withUsername(differentUsername);
        String json2 = objectMapper.writeValueAsString(user2);

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json2))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.CONFLICT_EMAIL.getMessage()));

        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
        assertNotEquals(users.getFirst().getEmail(), differentUsername);

    }

    @Test
    public void testCreateUserWithoutFieldsError() throws Exception {
        UserDTO invalidUser = differentUser.copy().withLocation(null);

        mockMvc.perform(post("/user")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_FIELDS.getMessage()));

        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
    }

    @Test
    public void testCreateUserWithShortUsername() throws Exception {
        UserDTO invalidUser = differentUser.copy().withUsername("ab");

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.SHORT_USERNAME.getMessage()));

        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
    }

    @Test
    public void testCreateUserWithShortPassword() throws Exception {
        UserDTO invalidUser = differentUser.copy().withPassword("1234567");

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.SHORT_PASSWORD.getMessage()));

        List<User> users = userRepository.findAll();
        assertEquals(1, users.size());
    }

    @Test
    public void testCreateUserWithInvalidEmail() throws Exception {
        UserDTO invalidUser = differentUser.copy().withEmail(".test@example.com");
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_EMAIL.getMessage()));

        invalidUser.setEmail("test@.com");
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_EMAIL.getMessage()));

        invalidUser.setEmail("test\"@example.com");
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_EMAIL.getMessage()));

        invalidUser.setEmail("test..test@example.com");
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_EMAIL.getMessage()));

        invalidUser.setEmail("\"weird email !#$%&'*+-/=?^_{|}~\"@example.com");
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidUser)))
                .andExpect(status().isOk());

        List<User> users = userRepository.findAll();
        assertEquals(2, users.size());
    }


    // USER LOGIN

    @Test
    public void testLoginWithEmailAndPassword() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO("", defaultUser.getPassword()).withEmail(defaultUser.getEmail());
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(createdUser.getAccessToken()));
    }

    @Test
    public void testLoginWithUsernameAndPassword() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), defaultUser.getPassword());
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(createdUser.getAccessToken()));
    }

    @Test
    public void testLoginWithEmailOnly() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO("", null).withEmail(defaultUser.getEmail());
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    public void testLoginWithUsernameOnly() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), null);
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    public void testLoginWithEmailAndInvalidPassword() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(createdUser.getEmail(), "wrongpassword");
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    public void testLoginWithUsernameAndInvalidPassword() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(createdUser.getUsername(), "wrongpassword");
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    public void testLoginWithUsernameAndWrongPasswordCapitalization() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), defaultUser.getPassword().toUpperCase());
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    public void testLoginWithEmailAndWrongPasswordCapitalization() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getEmail(), defaultUser.getPassword().toUpperCase());
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }
    @Test
    public void testLoginWithPasswordAndWrongUsernameCapitalization() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername().toUpperCase(), defaultUser.getPassword());
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    public void testLoginWithPasswordAndWrongEmailCapitalization() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getEmail().toUpperCase(), defaultUser.getPassword());
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    public void testLoginWithoutAnyFields() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(null, null);
        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist());
    }

    @Test
    public void testLoginWithValidToken() throws Exception {
        String token = createdUser.getAccessToken();
        mockMvc.perform(post("/user/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(createdUser.getUsername()));
    }

    @Test
    public void testLoginWithInvalidToken() throws Exception {
        String invalidToken = "invalidToken";
        mockMvc.perform(post("/user/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + invalidToken))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist());
    }

    // USER DELETION

    @Test
    public void testDeleteUserWithTokenEmailAndPassword() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(null, defaultUser.getPassword())
                .withEmail(defaultUser.getEmail());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.USER_DELETE_SUCCESS.getMessage()));

        Optional<User> deletedUser = userRepository.findById(createdUser.getId());
        assertFalse(deletedUser.isPresent(), "User should be deleted from the database");
    }

    @Test
    public void testDeleteUserWithTokenUsernameAndPassword() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), defaultUser.getPassword());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.USER_DELETE_SUCCESS.getMessage()));

        Optional<User> deletedUser = userRepository.findById(createdUser.getId());
        assertFalse(deletedUser.isPresent(), "User should be deleted from the database");
    }

    @Test
    public void testDeleteUserWithoutTokenWithEmailAndPassword() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(null, defaultUser.getPassword())
                .withEmail(defaultUser.getEmail());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted from the database");
    }

    @Test
    public void testDeleteUserWithoutTokenWithUsernameAndPassword() throws Exception {
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), defaultUser.getPassword());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted from the database");
    }

    @Test
    public void testDeleteUserWithTokenButWithoutPasswordUsingUsername() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), null);
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted from the database");
    }

    @Test
    public void testDeleteUserWithTokenButWithoutPasswordUsingEmail() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(null, null)
                .withEmail(defaultUser.getEmail());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value("error"));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted from the database");
    }

    @Test
    public void testCreateUserAfterDeletion() throws Exception {
        // Step 1: Delete the user
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), defaultUser.getPassword());
        String jsonDelete = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonDelete))
                .andExpect(status().isOk());

        Optional<User> deletedUser = userRepository.findById(createdUser.getId());
        assertFalse(deletedUser.isPresent(), "User should be deleted from the database");

        // Step 2: Create a new user with the same username and email
        UserDTO newUserDTO = new UserDTO(defaultUser.getUsername(), "newpassword", "newbio", "newlocation", defaultUser.getEmail());
        String jsonCreate = objectMapper.writeValueAsString(newUserDTO);

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonCreate))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(newUserDTO.getUsername()))
                .andExpect(jsonPath("$.email").value(newUserDTO.getEmail()));

        Optional<User> newUser = userRepository.findByUsername(newUserDTO.getUsername());
        assertTrue(newUser.isPresent(), "New user should be created in the database");
        assertEquals(newUserDTO.getEmail(), newUser.get().getEmail(), "New user should have the same email");
    }

    @Test
    public void testDeleteUserWithTokenAndIncorrectUsername() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO("wrongusername", defaultUser.getPassword());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted from the database");
    }

    @Test
    public void testDeleteUserWithTokenAndIncorrectEmail() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(null, defaultUser.getPassword())
                .withEmail("wrong@example.com");
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted from the database");
    }

    @Test
    public void testDeleteUserWithTokenAndUsernameAndIncorrectPassword() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), "wrongpassword");
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted from the database");
    }

    @Test
    public void testDeleteUserWithTokenAndEmailAndIncorrectPassword() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO("", "wrongpassword").withEmail(defaultUser.getEmail());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted from the database");
    }


    @Test
    public void testDeleteUserWithTokenAndUsernameDifferentCapitalization() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername().toUpperCase(), defaultUser.getPassword());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted due to case mismatch");
    }

    @Test
    public void testDeleteUserWithTokenAndEmailDifferentCapitalization() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(null, defaultUser.getPassword())
                .withEmail(defaultUser.getEmail().toUpperCase());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted due to case mismatch");
    }


    @Test
    public void testDeleteUserWithTokenAndUsernameAndPasswordWrongCapitalization() throws Exception {
        String token = createdUser.getAccessToken();
        UserLoginDTO loginInfo = new UserLoginDTO(defaultUser.getUsername(), defaultUser.getPassword().toUpperCase());
        String json = objectMapper.writeValueAsString(loginInfo);

        mockMvc.perform(delete("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));

        Optional<User> user = userRepository.findById(createdUser.getId());
        assertTrue(user.isPresent(), "User should not be deleted due to case mismatch");
    }

    // PATCH TESTS

    @Test
    public void testUpdateUserWithValidToken() throws Exception {
        UserDTO editedData = defaultUser.copy().withEmail("updatedemail@example.com").withBio("Updated bio").withLocation("Updated location").withUsername("UpdatedUsername");

        mockMvc.perform(patch("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdUser.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editedData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.USER_EDIT_SUCCESS.getMessage()));

        Optional<User> opt = userRepository.findById(createdUser.getId());
        assertTrue(opt.isPresent(), "User should be fetched with the same id after being edited");
        User user = opt.get();
        assertEquals(user.getUsername(), "UpdatedUsername");
        assertEquals(user.getEmail(), "updatedemail@example.com");
        assertEquals(user.getBio(), "Updated bio");
        assertEquals(user.getLocation(), "Updated location");
    }

    @Test
    public void testUpdateUserWithInvalidToken() throws Exception {
        UserDTO editedData = defaultUser.copy().withEmail("updatedemail@example.com").withBio("Updated bio").withLocation("Updated location").withUsername("UpdatedUsername");

        mockMvc.perform(patch("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer invalidToken")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editedData)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_TOKEN.getMessage()));

        Optional<User> opt = userRepository.findById(createdUser.getId());
        assertTrue(opt.isPresent(), "User should be fetched with the same id after not being edited");
        User user = opt.get();
        assertEquals(user.getUsername(), defaultUser.getUsername());
        assertEquals(user.getEmail(), defaultUser.getEmail());
        assertEquals(user.getBio(), defaultUser.getBio());
        assertEquals(user.getLocation(), defaultUser.getLocation());
    }

    @Test
    public void testUpdateUserWithValidTokenAndJustSomeFields() throws Exception {
        UserDTO editedData = defaultUser.copy().withEmail("updatedemail@example.com").withBio("Updated bio").withUsername(null).withLocation(null);

        mockMvc.perform(patch("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdUser.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editedData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.USER_EDIT_SUCCESS.getMessage()));

        Optional<User> opt = userRepository.findById(createdUser.getId());
        assertTrue(opt.isPresent(), "User should be fetched with the same id after being edited");
        User user = opt.get();
        assertEquals(user.getUsername(), defaultUser.getUsername());
        assertEquals(user.getEmail(), "updatedemail@example.com");
        assertEquals(user.getBio(), "Updated bio");
        assertEquals(user.getLocation(), defaultUser.getLocation());
    }

    @Test
    public void testUpdateUserHashAndTokenWithValidTokenAfterPasswordEdit() throws Exception {
        UserDTO editedData = defaultUser.copy().withPassword("DifferentPassword");

        mockMvc.perform(patch("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdUser.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(editedData)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.USER_EDIT_SUCCESS.getMessage()));

        Optional<User> opt = userRepository.findById(createdUser.getId());
        assertTrue(opt.isPresent(), "User should be fetched with the same id after being edited");
        User user = opt.get();

        assertNotEquals(user.getPasswordHash(), createdUser.getPasswordHash());
        assertNotEquals(user.getAccessToken(), createdUser.getAccessToken());

        // the new password should continue to work
        UserLoginDTO loginInfo = new UserLoginDTO(editedData.getUsername(), editedData.getPassword());

        mockMvc.perform(post("/user/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(loginInfo)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value(user.getAccessToken()));

        // the new token should also continue to work
        mockMvc.perform(post("/user/login")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + user.getAccessToken()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(user.getUsername()));
    }

    @Test
    public void testUpdateUserProtectedFieldsWithValidToken() throws Exception {
        String jsonData = "{\"role\":" + Role.ADMIN.ordinal() + "}";
        mockMvc.perform(patch("/user")
                        .header(HttpHeaders.AUTHORIZATION, "Bearer " + createdUser.getAccessToken())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(jsonData))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("success"))
                .andExpect(jsonPath("$.message").value(MessageStrings.USER_EDIT_SUCCESS.getMessage()));

        Optional<User> opt = userRepository.findById(createdUser.getId());
        assertTrue(opt.isPresent(), "User should be fetched with the same id after being edited with protected data");
        User user = opt.get();

        assertEquals(user.getRole(), createdUser.getRole());
        assertNotEquals(user.getRole(), Role.ADMIN);
    }


    @Test
    public void testGetUserById() throws Exception {
        mockMvc.perform(get("/user/" + createdUser.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(createdUser.getUsername()))
                .andExpect(jsonPath("$.bio").value(createdUser.getBio()))
                .andExpect(jsonPath("$.location").value(createdUser.getLocation()))
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    public void testGetUserWithInvalidId() throws Exception {
        mockMvc.perform(get("/user/102938219038"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_USER_ID.getMessage()))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.location").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

    @Test
    public void testGetUserWithoutId() throws Exception {
        mockMvc.perform(get("/user/"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value("error"))
                .andExpect(jsonPath("$.message").value(ErrorStrings.INVALID_PATH.getMessage()))
                .andExpect(jsonPath("$.username").doesNotExist())
                .andExpect(jsonPath("$.bio").doesNotExist())
                .andExpect(jsonPath("$.location").doesNotExist())
                .andExpect(jsonPath("$.email").doesNotExist())
                .andExpect(jsonPath("$.accessToken").doesNotExist())
                .andExpect(jsonPath("$.passwordHash").doesNotExist())
                .andExpect(jsonPath("$.role").doesNotExist());
    }

}