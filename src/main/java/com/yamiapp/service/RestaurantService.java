package com.yamiapp.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.yamiapp.exception.*;
import com.yamiapp.model.Restaurant;
import com.yamiapp.model.dto.RestaurantDTO;
import com.yamiapp.model.dto.RestaurantResposneDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.repo.RestaurantRepository;
import com.yamiapp.validator.RestaurantCreateValidator;
import com.yamiapp.validator.RestaurantUpdateRequestValidator;
import com.yamiapp.validator.UserLoginRequestValidator;
import jakarta.persistence.EntityNotFoundException;
import lombok.Setter;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.yamiapp.util.ServiceUtils.convertToJPEG;
import static com.yamiapp.util.ServiceUtils.validateModeratorUser;

import java.io.IOException;
import java.util.Optional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantCreateValidator createValidator;
    @Setter
    private BackblazeService backblazeService;
    private final UserService userService;
    private final RestaurantUpdateRequestValidator updateValidator;
    private final UserLoginRequestValidator loginValidator;

    public RestaurantService(
            final RestaurantRepository restaurantRepository,
            final UserService userService,
            final RestaurantCreateValidator createValidator,
            final BackblazeService backblazeService,
            final RestaurantUpdateRequestValidator updateValidator,
            final UserLoginRequestValidator loginValidator
    ) {
        this.restaurantRepository = restaurantRepository;
        this.userService = userService;
        this.createValidator = createValidator;
        this.backblazeService = backblazeService;
        this.updateValidator = updateValidator;
        this.loginValidator = loginValidator;
    }

    // will throw a B2Exception to be handled by B2ExceptionHandler
    @Transactional
    public Restaurant createRestaurant(RestaurantDTO dto, String userToken) throws B2Exception {
        createValidator.validate(dto);
        validateModeratorUser(userService, userToken);

        Restaurant r = new Restaurant();
        r.setName(dto.getName());
        r.setDescription(dto.getDescription());
        r.setShortName(dto.getShortName());

        try {
            r = restaurantRepository.save(r);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new ConflictException(ErrorStrings.CONFLICT_RESTAURANT_NAME.getMessage());
            }
            System.out.println(e.getMessage());
            throw new InternalServerException(ErrorStrings.INTEGRITY.getMessage());
        }

        // Process image and upload before updating the restaurant entity
        String photoName = r.getId().toString() + "/picture.jpg";

        try {
            MultipartFile jpegPhoto = dto.getPhoto().getContentType().equals("image/jpeg")
                    ? dto.getPhoto()
                    : convertToJPEG(dto.getPhoto());

            B2FileVersion photo = backblazeService.uploadFile(jpegPhoto, photoName);
            r.setPhotoPath(photoName);
            r.setPhotoId(photo.getFileId());

            restaurantRepository.save(r);
        } catch (IOException e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_IO.getMessage());
        }

        return r;
    }

    public Restaurant updateRestaurant(Integer id, RestaurantDTO dto, String userToken) throws B2Exception {
        updateValidator.validate(dto);
        validateModeratorUser(userService, userToken);

        Restaurant r = getRawById(id);

        if (dto.getName() != null) r.setName(dto.getName());
        if (dto.getDescription() != null) r.setDescription(dto.getDescription());
        if (dto.getShortName() != null) r.setShortName(dto.getShortName());
        if (dto.getPhoto() != null) {
            String photoName = r.getId().toString() + "/" + "picture.jpg";
            r.setPhotoPath(photoName);
            restaurantRepository.save(r);

            try {
                dto.setPhoto(convertToJPEG(dto.getPhoto()));
                backblazeService.uploadFile(dto.getPhoto(), photoName);
            } catch (IOException e) {
                throw new InternalServerException(ErrorStrings.INTERNAL_IO.getMessage());
            }
        }

        try {
            return restaurantRepository.save(r);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new ConflictException(ErrorStrings.CONFLICT_RESTAURANT_NAME.getMessage());
            }
            throw new InternalServerException(ErrorStrings.INTEGRITY.getMessage());
        }
    }

    public Page<RestaurantResposneDTO> searchRestaurantsUnauthenticated(String searchParams, Pageable page) {
        return restaurantRepository.getRestaurantsByAnonymousSearch(searchParams, page);
    }

    @Transactional
    public void deleteRestaurant(Integer id, String userToken, UserLoginDTO loginDTO) throws B2Exception {
        loginValidator.validate(loginDTO);
        validateModeratorUser(userService, userToken);
        String pwdToken = userService.getRawByPassword(loginDTO).getAccessToken();
        if (!pwdToken.equals(userToken)) {
            throw new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage());
        }

        Restaurant r = getRawById(id);

        backblazeService.deleteFile(r.getPhotoPath(), r.getPhotoId());
        restaurantRepository.delete(r);
    }

    public RestaurantResposneDTO getById(Integer id) {
        return new RestaurantResposneDTO(getRawById(id));
    }

    public Restaurant getRawById(Integer id) {
        return restaurantRepository.findById(id)
            .orElseThrow(() -> new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage()));
    }

    public Resource getImageById(Integer id) {
        try {
            Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(id);
            if (optionalRestaurant.isPresent()) {
                return backblazeService.downloadFile(optionalRestaurant.get().getPhotoPath());
            } else {
                throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
            }
        } catch (B2Exception e) {
            throw new InternalServerException(ErrorStrings.B2_UPSTREAM.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
        }
    }
}
