package com.food.project.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.project.exception.ErrorStrings;
import com.food.project.model.Role;
import com.food.project.model.User;
import com.food.project.model.dto.UserDTO;
import com.food.project.model.dto.UserLoginDTO;
import com.food.project.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
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


}
