package com.food.project.validator;

import com.food.project.exception.BadRequestException;
import com.food.project.exception.ErrorStrings;
import com.food.project.model.dto.RestaurantDTO;
import org.springframework.stereotype.Component;

@Component
public class RestaurantCreateValidator extends Validator<RestaurantDTO> {

    public RestaurantCreateValidator() {
        super();
    }

    @Override
    protected void initializeValidations() {
        ruleFor(r -> r.getName() != null, new BadRequestException(ErrorStrings.INVALID_FIELDS.getMessage()));
        ruleFor(r -> r.getName().length() >= 3, new BadRequestException(ErrorStrings.SHORT_RESTAURANT_NAME.getMessage()));
        ruleFor(r -> r.getPhoto() != null, new BadRequestException(ErrorStrings.INVALID_FIELDS.getMessage()));
        ruleFor(r -> r.getDescription() != null, new BadRequestException(ErrorStrings.INVALID_FIELDS.getMessage()));
        ruleFor(r -> {
            if (r.getPhoto() == null) {
                return false;
            }
            if (r.getPhoto().getContentType() == null) {
                return false;
            }
            return r.getPhoto().getContentType().startsWith("image");
        }, new BadRequestException(ErrorStrings.INVALID_IMAGE_FILETYPE.getMessage()));
    }
}
