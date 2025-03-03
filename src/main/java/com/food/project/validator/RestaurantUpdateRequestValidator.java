package com.food.project.validator;

import com.food.project.exception.BadRequestException;
import com.food.project.exception.ErrorStrings;
import com.food.project.model.dto.RestaurantDTO;
import org.springframework.stereotype.Component;

@Component
public class RestaurantUpdateRequestValidator extends Validator<RestaurantDTO>  {

    public RestaurantUpdateRequestValidator() {
        super();
    }

    @Override
    protected void initializeValidations() {
        ruleFor(r -> {
            if (r.getName() != null) {
                return r.getName().length() > 3;
            }
            return true;
        }, new BadRequestException(ErrorStrings.SHORT_RESTAURANT_NAME.getMessage()));
        ruleFor(r -> {
            if (r.getPhoto() != null) {
                if (r.getPhoto().getContentType() == null) {
                    return false;
                }
                return r.getPhoto().getContentType().startsWith("image");
            }
            return true;
        }, new BadRequestException(ErrorStrings.INVALID_IMAGE_FILETYPE.getMessage()));
    }
}
