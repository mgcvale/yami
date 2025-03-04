package com.yamiapp.validator;

import com.yamiapp.model.dto.FoodDTO;
import org.springframework.stereotype.Component;

@Component
public class FoodUpdateValidator extends Validator<FoodDTO> {

    public FoodUpdateValidator() {
        super();
    }

    @Override
    protected void initializeValidations() {

    }
}
