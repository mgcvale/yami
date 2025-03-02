package com.food.project.service;

import com.food.project.exception.*;
import com.food.project.model.Role;
import com.food.project.model.User;
import com.food.project.model.dto.UserDTO;
import com.food.project.model.dto.UserLoginDTO;
import com.food.project.model.dto.UserResponseDTO;
import com.food.project.repo.UserRepository;
import com.food.project.validator.UserCreateRequestValidator;
import com.food.project.validator.UserEditRequestValidator;
import com.food.project.validator.UserLoginRequestValidator;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final UserCreateRequestValidator createValidator = new UserCreateRequestValidator();
    private final UserEditRequestValidator editValidator = new UserEditRequestValidator();
    private final UserLoginRequestValidator loginValidator = new UserLoginRequestValidator();

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

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
        u.setEmail(dto.getEmail());
        u.setRole(Role.USER);

        // save on db
        try {
            return userRepository.save(u);
        } catch(DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                if (e.getMessage().contains("username_unique"))
                    throw new ConflictException(ErrorStrings.CONFLICT_USERNAME.getMessage());
                throw new ConflictException(ErrorStrings.CONFLICT_EMAIL.getMessage());
            }
            e.printStackTrace();
            throw new InternalServerException(ErrorStrings.INTEGRITY.getMessage());
        }
    }

    public User updateUser(User u, UserDTO dto) {
        editValidator.validate(dto);

        if (dto.getPassword() != null) {
            u.setPasswordHash(encoder.encode(dto.getPassword()));
            u.setAccessToken(UUID.randomUUID().toString());
        }
        if (dto.getUsername() != null) u.setUsername(dto.getUsername());
        if (dto.getBio() != null) u.setBio(dto.getBio());
        if (dto.getLocation() != null) u.setLocation(dto.getLocation());
        if (dto.getEmail() != null) u.setEmail(dto.getEmail());

        try {
            return userRepository.save(u);
        } catch(DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                if (e.getMessage().contains("username_unique"))
                    throw new ConflictException(ErrorStrings.CONFLICT_USERNAME.getMessage());
                throw new ConflictException(ErrorStrings.CONFLICT_EMAIL.getMessage());
            }
            throw new InternalServerException(ErrorStrings.INTEGRITY.getMessage());
        }
    }

    public User updateUser(String accessToken, UserDTO dto) {
        editValidator.validate(dto);

        try {
            Optional<User> optUser = userRepository.findByAccessToken(accessToken);
            if (optUser.isPresent()) {
                return updateUser(optUser.get(), dto);
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

    public void deleteUser(String accessToken, UserLoginDTO loginInfo) {
        loginValidator.validate(loginInfo);
        User found;

        try {
            Optional<User> u = userRepository.findByAccessToken(accessToken);
            if (u.isPresent()) {
                found = u.get();
            } else {
                throw new InternalServerException(ErrorStrings.INVALID_TOKEN.getMessage());
            }
        } catch (EntityNotFoundException e) {
            throw new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage());
        }

        if (
                encoder.matches(loginInfo.getPassword(), found.getPasswordHash()) &&
                (loginInfo.getUsername().equals(found.getUsername()) || loginInfo.getEmail().equals(found.getEmail()))
        ) {
            deleteUser(found);
        } else {
            throw new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage());
        }
    }

    public User getByToken(String accessToken) {
        try {
            Optional<User> u = userRepository.findByAccessToken(accessToken);
            if (u.isPresent()) {
                return u.get();
            } else {
                throw new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage());
            }
        } catch (EntityNotFoundException e) {
            throw new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage());
        }
    }

    public User getByPassword(UserLoginDTO loginInfo) {
        loginValidator.validate(loginInfo);

        try {
            Optional<User> optUser = userRepository.findByUsernameOrEmail(loginInfo.getUsername(), loginInfo.getEmail());
            if (optUser.isPresent()) {
                var u = optUser.get();
                if (encoder.matches(loginInfo.getPassword(), u.getPasswordHash())) {
                    return u;
                }
                throw new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage());
            }
            throw new UnauthorizedException(loginInfo.getEmail().isEmpty() ? ErrorStrings.INVALID_USERNAME.getMessage() : ErrorStrings.INVALID_EMAIL.getMessage());
        } catch (EntityNotFoundException e) {
            throw new UnauthorizedException(loginInfo.getEmail().isEmpty() ? ErrorStrings.INVALID_USERNAME.getMessage() : ErrorStrings.INVALID_EMAIL.getMessage());
        }
    }

    public UserResponseDTO getById(Long id) {
        try {
            Optional<User> optUser = userRepository.findById(id);
            if (optUser.isPresent()) {
                return new UserResponseDTO(optUser.get());
            } else {
                throw new NotFoundException(ErrorStrings.INVALID_USER_ID.getMessage());
            }
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(ErrorStrings.INVALID_USER_ID.getMessage());
        } catch (NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
        }
    }

}
