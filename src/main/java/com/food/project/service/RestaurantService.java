package com.food.project.service;

import com.backblaze.b2.client.exceptions.B2Exception;
import com.food.project.exception.*;
import com.food.project.model.Restaurant;
import com.food.project.model.Role;
import com.food.project.model.User;
import com.food.project.model.dto.RestaurantDTO;
import com.food.project.model.dto.RestaurantResposneDTO;
import com.food.project.repo.RestaurantRepository;
import com.food.project.util.ByteArrayMultipartFile;
import com.food.project.validator.RestaurantCreateValidator;
import jakarta.persistence.EntityNotFoundException;
import org.apache.http.entity.ContentType;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
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

    public RestaurantService(
            final RestaurantRepository restaurantRepository,
            final UserService userService,
            final RestaurantCreateValidator createValidator,
            final BackblazeService backblazeService
    ) {
        this.restaurantRepository = restaurantRepository;
        this.userService = userService;
        this.createValidator = createValidator;
        this.backblazeService = backblazeService;
    }

    private void validateUser(String userToken) {
        User u = userService.getByToken(userToken);
        if (u.getRole() != Role.ADMIN && u.getRole() != Role.MODERATOR) {
            throw new ForbiddenException(ErrorStrings.FORBIDDEN_NOT_ADMIN.getMessage());
        }
    }

    // will throw a B2Exception to be handled by B2ExceptionHandler
    public Restaurant createRestaurant(RestaurantDTO dto, String userToken) throws B2Exception {
        createValidator.validate(dto);
        validateUser(userToken);

        var r = new Restaurant();
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

        // now that we created the restaurant, we need to set its photo path and store the photo in backblaze
        String photoName = r.getId().toString() + "/" + "picture.jpg";
        r.setPhotoPath(photoName);
        restaurantRepository.save(r);

        // convert the picture to jpeg and save it in backblaze
        try {
            dto.setPhoto(convertToJPEG(dto.getPhoto()));
            backblazeService.uploadFile(dto.getPhoto(), photoName);
        } catch (IOException e) {
            throw new InternalServerException(ErrorStrings.INTERNAL_IO.getMessage());
        }

        return r;
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
