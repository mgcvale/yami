package com.food.project.validator;

import com.food.project.exception.BadRequestException;
import com.food.project.exception.UnauthorizedException;
import com.food.project.model.dto.UserLoginDTO;

public class UserLoginRequestValidator extends Validator<UserLoginDTO> {
    @Override
    protected void initializeValidations() {
        ruleFor(u -> u.getPassword() != null, new BadRequestException("The password can't be null"));
        ruleFor(u -> u.getUsername() != null, new BadRequestException("The username can't be null"));
    }
}
