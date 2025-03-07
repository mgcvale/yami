package com.yamiapp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.repo.UserRepository;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

public class TestUtils {

    public static User createUserWithRole(MockMvc mockMvc, ObjectMapper objectMapper, UserRepository userRepository, UserDTO userDTO, Role role) throws Exception {
        String json = objectMapper.writeValueAsString(userDTO);
        mockMvc.perform(post("/user")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json))
                .andExpect(status().isOk());

        Optional<User> savedUser = userRepository.findByUsername(userDTO.getUsername());
        assertTrue(savedUser.isPresent());
        User user = savedUser.get();
        user.setRole(role);
        return userRepository.save(user);
    }

}
