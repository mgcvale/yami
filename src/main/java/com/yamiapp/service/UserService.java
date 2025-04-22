package com.yamiapp.service;

import com.yamiapp.exception.*;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.UserCountsDTO;
import com.yamiapp.model.dto.UserDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.model.dto.UserResponseDTO;
import com.yamiapp.repo.UserRepository;
import com.yamiapp.validator.UserCreateRequestValidator;
import com.yamiapp.validator.UserEditRequestValidator;
import com.yamiapp.validator.UserLoginRequestValidator;
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

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder;
    private final UserCreateRequestValidator createValidator;
    private final UserEditRequestValidator editValidator;
    private final UserLoginRequestValidator loginValidator;

    @Autowired
    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
        this.encoder = new BCryptPasswordEncoder();
        this.createValidator = new UserCreateRequestValidator();
        this.editValidator = new UserEditRequestValidator();
        this.loginValidator = new UserLoginRequestValidator();
    }
    
    public User createRawUser(UserDTO dto) {
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
                System.out.println("Constraint name: " + ((ConstraintViolationException) e.getCause()).getConstraintName());
                if (((ConstraintViolationException) e.getCause()).getConstraintName().contains("username_unique"))
                    throw new ConflictException(ErrorStrings.CONFLICT_USERNAME.getMessage());
                throw new ConflictException(ErrorStrings.CONFLICT_EMAIL.getMessage());
            }
            e.printStackTrace();
            throw new InternalServerException(ErrorStrings.INTEGRITY.getMessage());
        }
    }

    public User updateRawUser(User u, UserDTO dto) {
        editValidator.validate(dto);

        if (dto.getPassword() != null) {
            System.out.println("Password is not null");
            u.setPasswordHash(encoder.encode(dto.getPassword()));
            u.setAccessToken(UUID.randomUUID().toString());
        }
        if (dto.getUsername() != null) u.setUsername(dto.getUsername());
        if (dto.getBio() != null) u.setBio(dto.getBio());
        if (dto.getLocation() != null) u.setLocation(dto.getLocation());
        if (dto.getEmail() != null) u.setEmail(dto.getEmail());

        try {
            User newU = userRepository.save(u);
            return newU;
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                if (e.getMessage().contains("username_unique"))
                    throw new ConflictException(ErrorStrings.CONFLICT_USERNAME.getMessage());
                throw new ConflictException(ErrorStrings.CONFLICT_EMAIL.getMessage());
            }
            throw new InternalServerException(ErrorStrings.INTEGRITY.getMessage());
        }
    }

    public User updateRawUser(String accessToken, UserDTO dto) {
        editValidator.validate(dto);

        try {
            Optional<User> optUser = userRepository.findByAccessToken(accessToken);
            if (optUser.isPresent()) {
                return updateRawUser(optUser.get(), dto);
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

    public User getRawByToken(String accessToken) {
        if (accessToken == null) {
            throw new UnauthorizedException(ErrorStrings.INVALID_TOKEN.getMessage());
        }
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

    public UserResponseDTO getByToken(String accessToken) {
        return new UserResponseDTO(getRawByToken(accessToken));
    }

    public User getRawByPassword(UserLoginDTO loginInfo) {
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

    public UserResponseDTO getByPassword(UserLoginDTO loginDTO) {
        return new UserResponseDTO(getRawByPassword(loginDTO));
    }

    public User getRawById(Long id) {
        try {
            Optional<User> optUser = userRepository.findById(id);
            if (optUser.isPresent()) {
                return optUser.get();
            } else {
                throw new NotFoundException(ErrorStrings.INVALID_USER_ID.getMessage());
            }
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(ErrorStrings.INVALID_USER_ID.getMessage());
        } catch (NotFoundException e) {
            throw e;
        } catch (Exception e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
        }
    }

    public UserCountsDTO getUserCounts(Long userId) {
        try {
            return userRepository.getUserCounts(userId);
        } catch (Exception e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
        }
    }

    public UserResponseDTO getById(Long id) {
        return new UserResponseDTO(getRawById(id));
    }

    public boolean userExists(Long userId) {
        return userRepository.existsById(Math.toIntExact(userId));
    }

}
