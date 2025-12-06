package com.yamiapp.service;

import com.backblaze.b2.client.exceptions.B2BadRequestException;
import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.yamiapp.exception.*;
import com.yamiapp.model.Food;
import com.yamiapp.model.FoodReview;
import com.yamiapp.model.Restaurant;
import com.yamiapp.model.dto.*;
import com.yamiapp.model.projection.FoodWithReviewProjection;
import com.yamiapp.repo.FoodRepository;
import com.yamiapp.repo.RestaurantRepository;
import com.yamiapp.util.ResponseFactory;
import com.yamiapp.validator.FoodCreateValidator;
import com.yamiapp.validator.FoodUpdateValidator;
import com.yamiapp.validator.UserLoginRequestValidator;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.yamiapp.util.ServiceUtils.convertToJPEG;
import static com.yamiapp.util.ServiceUtils.validateModeratorUser;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final BackblazeService backblazeService;
    private final FoodCreateValidator createValidator;
    private final UserService userService;
    private final RestaurantService restaurantService;
    private final FoodUpdateValidator updateValidator;
    private final UserLoginRequestValidator userLoginRequestValidator;

    public FoodService(
            final FoodRepository foodRepository,
            final BackblazeService backblazeService,
            final FoodCreateValidator createValidator,
            final UserService userService,
            RestaurantService restaurantService,
            UserLoginRequestValidator userLoginRequestValidator,
            FoodUpdateValidator foodUpdateValidator) {
        this.foodRepository = foodRepository;
        this.backblazeService = backblazeService;
        this.createValidator = createValidator;
        this.userService = userService;
        this.restaurantService = restaurantService;
        this.updateValidator = foodUpdateValidator;
        this.userLoginRequestValidator = userLoginRequestValidator;
    }


    @Transactional
    public Food createFood(FoodDTO foodDTO, String accessToken) throws B2Exception {
        validateModeratorUser(userService, accessToken);
        createValidator.validate(foodDTO);

        // get restaurant from ID
        Restaurant r = restaurantService.getRawById(foodDTO.getRestaurantId());

        Food f = new Food();
        f.setName(foodDTO.getName());
        f.setRestaurant(r);
        f.setDescription(foodDTO.getDescription());

        try {
            f = foodRepository.save(f);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new ConflictException(ErrorStrings.CONFLICT_FOOD_NAME.getMessage());
            }
            e.printStackTrace();
            throw new InternalServerException(ErrorStrings.INTEGRITY.getMessage());
        }

        if (foodDTO.getPhoto() != null) {
            String photoName = r.getId().toString() + "/food/" + f.getId().toString() + ".jpg";

            try {
                MultipartFile jpegPhoto = foodDTO.getPhoto().getContentType().equals("image/jpeg")
                        ? foodDTO.getPhoto()
                        : convertToJPEG(foodDTO.getPhoto());

                B2FileVersion photo = backblazeService.uploadFile(jpegPhoto, photoName);
                f.setPhotoPath(photoName);
                f.setPhotoId(photo.getFileId());

                foodRepository.save(f);
            } catch (IOException e) {
                throw new InternalServerException(ErrorStrings.INTERNAL_IO.getMessage());
            }
        }

        return f;
    }

    @Transactional
    public Food updateFood(Long id, FoodDTO foodDTO, String accessToken) throws B2Exception {
        updateValidator.validate(foodDTO);
        validateModeratorUser(userService, accessToken);

        Food f = getRawById(id);

        if (foodDTO.getName() != null) f.setName(foodDTO.getName());
        if (foodDTO.getDescription() != null) f.setDescription(foodDTO.getDescription());
        if (foodDTO.getPhoto() != null) {
            String photoName = f.getRestaurant().getId().toString() + "/food/" + f.getId().toString() + ".jpg";

            try {
                MultipartFile jpegPhoto = foodDTO.getPhoto().getContentType().equals("image/jpeg")
                        ? foodDTO.getPhoto()
                        : convertToJPEG(foodDTO.getPhoto());

                B2FileVersion photo = backblazeService.uploadFile(jpegPhoto, photoName);
                f.setPhotoPath(photoName);
                f.setPhotoId(photo.getFileId());

                foodRepository.save(f);
            } catch (IOException e) {
                throw new InternalServerException(ErrorStrings.INTERNAL_IO.getMessage());
            }
        }

        return f;
    }

    @Transactional
    public void deleteFood(Long id, String userToken, UserLoginDTO loginDTO) throws B2Exception {
        userLoginRequestValidator.validate(loginDTO);
        validateModeratorUser(userService, userToken);
        String pwdToken = userService.getRawByPassword(loginDTO).getAccessToken();
        if (!pwdToken.equals(userToken)) {
            throw new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage());
        }

        Food f = getRawById(id);

        if (f.getPhotoId() != null) {
            try {
                backblazeService.deleteFile(f.getPhotoPath(), f.getPhotoId());
            } catch (B2BadRequestException e) {
                if (!e.getMessage().toLowerCase().contains("not present")) {
                    throw e;
                }
                System.out.println("Not present");
                // we will ignore file not present errors, as foods are allowed not to have a photo
            }
        } // if b2 threw an exception, we will not delete the food.

        foodRepository.delete(f);
    }

    public Map<Integer, Long> getFoodStats(Long id) {
        if (!foodRepository.existsById(id)) {
            throw new NotFoundException(ErrorStrings.INVALID_FOOD_ID.getMessage());
        }

        try {
            List<RatingDistributionEntry> ratingDistributionEntries = foodRepository.getRatingDistribution(id);

            Map<Integer, Long> ratingMap = IntStream.rangeClosed(0, 20)
                .boxed()
                .collect(Collectors.toMap(
                    key -> key,
                    value -> 0L
                ));

            ratingDistributionEntries.forEach(entry -> {
                if (entry.key() != null && entry.value() != null) {
                    ratingMap.put(entry.key(), entry.value());
                }
            });

            return ratingMap;
        } catch (Exception e) {
            throw e;
        }
    }

    public FoodResponseDTO getById(Long id) {
        return new FoodResponseDTO(getRawById(id));
    }

    public FoodResponseDTO getByIdAuthenticated(Long id, String token) {
        FoodWithReviewProjection projection = foodRepository.findFoodByIdWithUserReviewProjection(id, token)
            .orElseThrow(() -> new NotFoundException(ErrorStrings.INVALID_FOOD_ID.getMessage()));

        FoodResponseDTO response = new FoodResponseDTO(
            projection.foodId(),
            projection.foodName(),
            projection.foodDescription(),
            Math.toIntExact(projection.restaurantId()),
            projection.restaurantName(),
            projection.restaurantShortName(),
            projection.avgRating()
        );

        if (projection.userReview() != null && projection.userRating() != null) {
            response.setReview(new FoodResponseDTO.EmbeddedReview(
                projection.reviewId(),
                projection.userReview(),
                projection.userRating()
            ));
        }

        return response;
    }

    public Food getRawById(Long id) {
        return foodRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorStrings.INVALID_FOOD_ID.getMessage()));
    }

    public double getAverageRating(Long id) {
        return foodRepository.getAverageRating(id);
    }

    public Resource getImageById(Long id) throws B2Exception {

        Food f = getRawById(id);
        String photoPath = f.getPhotoPath();
        if (photoPath == null) {
            throw new NotFoundException(ErrorStrings.FOOD_DOESNT_HAVE_PHOTO.getMessage());
        }

        return backblazeService.downloadFile(photoPath);
    }


    public List<Food> getRawByRestaurantId(Long id) {
        return foodRepository.getRestaurantFoods(id);
    }

    public List<FoodResponseDTO> getByRestaurantId(Long id) {
        return getRawByRestaurantId(id).stream().map(FoodResponseDTO::new).toList();
    }

    public List<Food> searchRawRestaurantFoods(Long id, String searchQuery) {
        return foodRepository.searchRestaurantFoods(id, searchQuery);
    }

    public List<FoodResponseDTO> searchRestaurantFoods(Long id, String searchQuery) {
        return searchRawRestaurantFoods(id, searchQuery).stream().map(FoodResponseDTO::new).toList();
    }

}
