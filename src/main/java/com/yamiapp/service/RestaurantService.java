package com.yamiapp.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.backblaze.b2.client.structures.B2FileVersion;
import com.yamiapp.exception.*;
import com.yamiapp.model.Restaurant;
import com.yamiapp.model.Role;
import com.yamiapp.model.User;
import com.yamiapp.model.dto.RestaurantDTO;
import com.yamiapp.model.dto.RestaurantResposneDTO;
import com.yamiapp.model.dto.UserLoginDTO;
import com.yamiapp.repo.RestaurantRepository;
import com.yamiapp.util.ByteArrayMultipartFile;
import com.yamiapp.validator.RestaurantCreateValidator;
import com.yamiapp.validator.RestaurantUpdateRequestValidator;
import com.yamiapp.validator.UserLoginRequestValidator;
import jakarta.persistence.EntityNotFoundException;
import org.apache.http.entity.ContentType;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Optional;

@Service
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final RestaurantCreateValidator createValidator;
    private final BackblazeService backblazeService;
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

    private void validateUser(String userToken) {
        User u = userService.getByToken(userToken);
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.MODERATOR) {
            throw new ForbiddenException(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage());
        }
    }

    // will throw a B2Exception to be handled by B2ExceptionHandler
    @Transactional
    public Restaurant createRestaurant(RestaurantDTO dto, String userToken) throws B2Exception {
        createValidator.validate(dto);
        validateUser(userToken);

        Restaurant r = new Restaurant();
        r.setName(dto.getName());
        r.setDescription(dto.getDescription());

        try {
            r = restaurantRepository.save(r);
        } catch (DataIntegrityViolationException e) {
            if (e.getCause() instanceof ConstraintViolationException) {
                throw new ConflictException(ErrorStrings.CONFLICT_RESTAURANT_NAME.getMessage());
            }
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
        validateUser(userToken);

        Restaurant r;
        try {
            Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(id);
            if (optionalRestaurant.isEmpty()) {
                throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
            }
            r = optionalRestaurant.get();
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
        } catch (NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
        }

        if (dto.getName() != null) r.setName(dto.getName());
        if (dto.getDescription() != null) r.setDescription(dto.getDescription());
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

    @Transactional
    public void deleteRestaurant(Integer id, String userToken, UserLoginDTO loginDTO) throws B2Exception {
        loginValidator.validate(loginDTO);

        Restaurant r;

        try {
            Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(id);
            if (optionalRestaurant.isEmpty()) {
                throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
            }
            r = optionalRestaurant.get();
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
        } catch (NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
        }

        backblazeService.deleteFile(r.getPhotoPath(), r.getPhotoId());
        restaurantRepository.delete(r);
    }

    public RestaurantResposneDTO getById(Integer id) {
        try {
            Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(id);
            if (optionalRestaurant.isPresent()) {
                return new RestaurantResposneDTO(optionalRestaurant.get());
            } else {
                throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
            }
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
        } catch (NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
        }
    }

    public Resource getImageById(Integer id) {
        try {
            Optional<Restaurant> optionalRestaurant = restaurantRepository.findById(id);
            if (optionalRestaurant.isPresent()) {
                return backblazeService.downloadFile(optionalRestaurant.get().getPhotoPath());
            } else {
                throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
            }
        } catch (EntityNotFoundException e) {
            throw new NotFoundException(ErrorStrings.INVALID_RESTAURANT_ID.getMessage());
        } catch (NotFoundException e) {
            throw new NotFoundException(e.getMessage());
        } catch (Exception e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
        }
    }

    private MultipartFile convertToJPEG(MultipartFile multipartFile) throws IOException {
        byte[] fileBytes = multipartFile.getBytes();

        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(fileBytes);
        BufferedImage bufferedImage = ImageIO.read(byteArrayInputStream);

        if (bufferedImage == null) {
            throw new BadRequestException(ErrorStrings.INVALID_IMAGE_FILETYPE.getMessage());
        }

        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ImageIO.write(bufferedImage, "JPEG", byteArrayOutputStream);
        return  new ByteArrayMultipartFile(byteArrayOutputStream.toByteArray(), multipartFile.getName(), multipartFile.getOriginalFilename(), ContentType.IMAGE_JPEG.getMimeType());
    }

}
