package com.food.project.user;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.food.project.model.Role;
import com.food.project.model.User;
import com.food.project.model.dto.UserDTO;
import com.food.project.repo.UserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

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

    @AfterEach
    public void cleanup() {
        userRepository.deleteAll();
    }

    @Test
    public void testCreateUserSuccess() throws Exception {
        UserDTO userDTO = new UserDTO("testuser", "password123", "bio", "location", "test@example.com");
        String json = objectMapper.writeValueAsString(userDTO);

        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("testuser"))
                .andExpect(jsonPath("$.email").value("test@example.com"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.id").exists());

        Optional<User> savedUser = userRepository.findByUsername("testuser");
        assertTrue(savedUser.isPresent());
        User user = savedUser.get();
        assertEquals("testuser", user.getUsername());
        assertEquals("test@example.com", user.getEmail());
        assertTrue(encoder.matches("password123", user.getPasswordHash()));
        assertNotNull(user.getAccessToken());
        assertEquals(Role.USER, user.getRole());
    }

}
