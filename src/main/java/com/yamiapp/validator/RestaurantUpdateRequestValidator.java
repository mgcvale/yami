package com.yamiapp.validator;

import com.yamiapp.exception.BadRequestException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.model.dto.RestaurantDTO;
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
                System.out.println(r.getPhoto().getContentType());
                return r.getPhoto().getContentType().startsWith("image");
            }
            return true;
        }, new BadRequestException(ErrorStrings.INVALID_IMAGE_FILETYPE.getMessage()));
        ruleFor(r -> {
            if (r.getPhoto() != null) {
                if (r.getPhoto().getSize() > 5 * 1024 * 1024) {
                    return false;
                }
            }
            return true;
        }, new BadRequestException(ErrorStrings.FILE_TOO_LARGE.getMessage()));
    }
}
