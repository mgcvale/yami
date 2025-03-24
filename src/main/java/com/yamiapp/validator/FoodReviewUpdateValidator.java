package com.yamiapp.validator;

import com.yamiapp.exception.BadRequestException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.model.dto.FoodReviewDTO;
import org.springframework.stereotype.Component;

@Component
public class FoodReviewUpdateValidator extends Validator<FoodReviewDTO> {

    public FoodReviewUpdateValidator() {
        super();
    }

    @Override
    protected void initializeValidations() {
        ruleFor(fr -> {
            if (fr.getRating() != null) {
                return fr.getRating() >= 0 && fr.getRating() <= 20;
            }
            return true;
        }, new BadRequestException(ErrorStrings.BAD_FOOD_REVIEW_RATING.getMessage()));
        ruleFor(fr -> {
            if (fr.getReview() != null) {
                return fr.getReview().length() >= 2 && fr.getReview().length() <= 512;
            }
            return true;
        }, new BadRequestException(ErrorStrings.BAD_FOOD_REVIEW_LENGTH.getMessage()));
    }
}
