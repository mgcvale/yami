package com.food.project.service;

import com.food.project.exception.*;
import com.food.project.model.User;
import com.food.project.model.dto.UserDTO;
import com.food.project.model.dto.UserLoginDTO;
import com.food.project.repo.UserRepository;
import com.food.project.validator.UserCreateRequestValidator;
import com.food.project.validator.UserEditRequestValidator;
import com.food.project.validator.UserLoginRequestValidator;
import jakarta.persistence.EntityNotFoundException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;
import java.util.UUID;

@Service
public class UserService {

    @Autowired
    private UserRepository userRepository;

    private BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private UserCreateRequestValidator createValidator = new UserCreateRequestValidator();
    private UserEditRequestValidator editValidator = new UserEditRequestValidator();
    private UserLoginRequestValidator loginValidator = new UserLoginRequestValidator();

    public User createUser(UserDTO dto) {
        // validate dto first
        createValidator.validate(dto);

        // properly create user
        var u = new User();
        u.setUsername(dto.getUsername());
        u.setBio(dto.getBio());
        u.setLocation(dto.getLocation());
        u.setPasswordHash(encoder.encode(dto.getPassword()));
        u.setAccessToken(UUID.randomUUID().toString());

        // save on db
        try {
            return userRepository.save(u);
        } catch(DataIntegrityViolationException e) {
            throw new ConflictException(ErrorStrings.CONFLICT_USERNAME.getMessage());
        }
    }

    public User updateUser(User u, UserDTO dto) {
        editValidator.validate(dto);

        if (dto.getPassword() != null) {
            u.setPasswordHash(encoder.encode(dto.getPassword()));
            u.setAccessToken(UUID.randomUUID().toString());
        }
        if (dto.getUsername() != null) u.setUsername(dto.getUsername());
        if (dto.getBio() != null) u.setUsername(dto.getUsername());
        if (dto.getLocation() != null) u.setLocation(dto.getLocation());

        try {
            return userRepository.save(u);
        } catch(DataIntegrityViolationException e) {
            throw new ConflictException(ErrorStrings.CONFLICT_USERNAME.getMessage());
        }
    }

    public User updateUser(String accessToken, UserDTO dto) {
        editValidator.validate(dto);

        try {
            Optional<User> optUser = userRepository.findByAccessToken(accessToken);
            if (optUser.isPresent()) {
                try {
                    return updateUser(optUser.get(), dto);
                } catch(Exception e) {
                    throw e;
                }
            } else {
                throw new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage());
            }
        } catch (Exception e) {
            throw new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage());
        }
    }

    //TODO: optimize by deleting via access token
    public void deleteUser(User u) {
        try {
            userRepository.delete(u);
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(ErrorStrings.NO_USER_FOUND.getMessage());
        }
    }

    public User getByToken(String accessToken) {
        try {
            Optional<User> u = userRepository.findByAccessToken(accessToken);
            if (u.isPresent()) {
                return u.get();
            } else {
                throw new InternalServerException(ErrorStrings.INVALID_TOKEN.getMessage());
            }
        } catch (EntityNotFoundException e) {
            throw new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage());
        }
    }

    public User getByPassword(UserLoginDTO loginInfo) {
        loginValidator.validate(loginInfo);

        try {
            Optional<User> optUser = userRepository.findByUsername(loginInfo.getUsername());
            if (optUser.isPresent()) {
                var u = optUser.get();
                if (encoder.matches(loginInfo.getPassword(), u.getPasswordHash())) {
                    return u;
                }
                throw new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage());
            }
            throw new InternalServerException(ErrorStrings.INTERNAL_NO_RESULT.getMessage());
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(ErrorStrings.INVALID_USERNAME.getMessage());
        }
    }

}
