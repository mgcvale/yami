package com.yamiapp.service;

import com.yamiapp.exception.*;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.*;
import com.yamiapp.repo.UserRepository;
import com.yamiapp.validator.UserCreateRequestValidator;
import com.yamiapp.validator.UserEditRequestValidator;
import com.yamiapp.validator.UserLoginRequestValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

@Slf4j
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
        u.setEmail(dto.getEmail().toLowerCase());
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
        if (dto.getEmail() != null) u.setEmail(dto.getEmail().toLowerCase());

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

    public UserStats getUserStats(Long userId) {
        if (!userExists(userId)) {
            throw new NotFoundException(ErrorStrings.INVALID_USER_ID.getMessage());
        }

        try {
            List<RatingDistributionEntry> ratingDistributionEntries = userRepository.getRatingDistribution(userId);
            System.out.println("DISTRIBUTION: " + ratingDistributionEntries);
            Double avgRating = userRepository.getAverageRating(userId);
            avgRating = (avgRating == null) ? 0D : avgRating;

            Map<Integer, Long> ratingMap = IntStream.rangeClosed(0, 20)
                    .boxed()
                    .collect(Collectors.toMap(
                            key -> key,
                            key -> 0L
                    ));

            ratingDistributionEntries.forEach(entry -> {
                if (entry.key() != null && entry.value() != null) {
                    ratingMap.put(entry.key(), entry.value());
                }
            });

            return new UserStats(
                    avgRating,
                    ratingMap
            );
        } catch (Exception e) {
            throw e;
        }
    }

    public UserResponseDTO getById(Long id) {
        return new UserResponseDTO(getRawById(id));
    }

    public boolean userExists(Long userId) {
        return userRepository.existsById(userId);
    }

    public Page<UserResponseDTO> searchUsersUnauthenticated(String searchParams, Pageable pageable) {
        Page<User> users = userRepository.getUsersByAnonymousSearch("%" + searchParams + "%", pageable);
        return users.map(user -> new UserResponseDTO(user).withoutSensitiveData());
    }

    public Page<UserResponseDTO> searchUsersAuthenticated(String searchParams, String accessToken, Pageable pageable) {
        User u;
        try{
            u = getRawByToken(accessToken);
        } catch (UnauthorizedException | NotFoundException e) {
            log.warn("Failed to authenticate user in searchusersAuthenticating; falling back to searchUsersUnauthenticated");
            return searchUsersUnauthenticated(searchParams, pageable);
        }

        searchParams = "%" + searchParams + "%";
        int pageSize = pageable.getPageSize();
        int pageNumber = pageable.getPageNumber();

        Page<User> second = userRepository.findSecondDegree(u.getId(), searchParams, PageRequest.of(pageNumber, pageSize / 4));
        Page<User> shared = userRepository.findSharedInterest(u.getId(), searchParams, PageRequest.of(pageNumber, pageSize / 4));
        Page<User> popular = userRepository.findPopularUsersExcludingFollows(u.getId(), searchParams, PageRequest.of(pageNumber, pageSize / 4));
        Page<User> general = userRepository.getUsersByAnonymousSearch(searchParams, PageRequest.of(pageNumber, pageSize / 4));
        List<UserResponseDTO> result = new ArrayList<>();

        Set<Long> userIds = new HashSet<>();

        Consumer<Page<User>> addToResult = page -> {
            for (User user : page.getContent()) {
                if (result.size() >= pageSize || userIds.contains(user.getId())) {
                    continue;
                }
                result.add(new UserResponseDTO(user).withoutSensitiveData());
                userIds.add(user.getId());
            }
        };

        addToResult.accept(second);
        addToResult.accept(shared);
        addToResult.accept(popular);
        addToResult.accept(general);

        long total = second.getTotalElements() + shared.getTotalElements() + popular.getTotalElements() + general.getTotalElements();
        return new PageImpl<>(result, pageable, total);
    }

}
