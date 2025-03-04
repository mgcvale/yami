package com.yamiapp.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.yamiapp.exception.ConflictException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.ForbiddenException;
import com.yamiapp.exception.InternalServerException;
import com.yamiapp.model.Food;
import com.yamiapp.model.Restaurant;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.FoodDTO;
import com.yamiapp.repo.FoodRepository;
import com.yamiapp.validator.FoodCreateValidator;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

import static com.yamiapp.util.ServiceUtils.convertToJPEG;

@Service
public class FoodService {

    private final FoodRepository foodRepository;
    private final BackblazeService backblazeService;
    private final FoodCreateValidator createValidator;
    private final UserService userService;
    private final RestaurantService restaurantService;

    public FoodService(
            final FoodRepository foodRepository,
            final BackblazeService backblazeService,
            final FoodCreateValidator createValidator,
            final UserService userService,
            RestaurantService restaurantService
    ) {
        this.foodRepository = foodRepository;
        this.backblazeService = backblazeService;
        this.createValidator = createValidator;
        this.userService = userService;
        this.restaurantService = restaurantService;
    }

    private void validateUser(String userToken) {
        User u = userService.getByToken(userToken);
        if (u.getRole().ordinal() <= Role.PRO_USER.ordinal()) {
            throw new ForbiddenException(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage());
        }
    }

    public Food createFood(FoodDTO foodDTO, String accessToken) throws B2Exception {
        validateUser(accessToken);
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

    public Food updateFood() { return null; }


}
