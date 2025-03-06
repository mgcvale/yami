package com.yamiapp.validator;

import com.yamiapp.exception.BadRequestException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.model.dto.FoodDTO;
import org.springframework.stereotype.Component;

@Component
public class FoodUpdateValidator extends Validator<FoodDTO> {

    public FoodUpdateValidator() {
        super();
    }

    @Override
    protected void initializeValidations() {

        ruleFor(f -> {
            if (f.getPhoto() == null) {
                return true; // photos can be null
            }
            if (f.getPhoto().getContentType() == null) {
                return false;
            }
            return f.getPhoto().getContentType().startsWith("image");
        }, new BadRequestException(ErrorStrings.INVALID_IMAGE_FILETYPE.getMessage()));

        ruleFor(f -> {
            if (f.getPhoto() == null) {
                return true; // photos can be null
            }
            if (f.getPhoto().getSize() > 5 * 1024 * 1024) {
                return false;
            }
            return true;
        }, new BadRequestException(ErrorStrings.FILE_TOO_LARGE.getMessage()));
    }
}
