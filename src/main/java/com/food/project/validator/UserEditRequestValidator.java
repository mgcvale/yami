package com.food.project.validator;

import com.food.project.exception.BadRequestException;
import com.food.project.model.dto.UserDTO;

public class UserEditRequestValidator extends Validator<UserDTO> {
    @Override
    protected void initializeValidations() {
        ruleFor(u -> {
            if (u.getPassword() != null) {
                return u.getPassword().length() >= 8;
            }
            return true;
        }, new BadRequestException("The password must be at least 8 characters long"));
        ruleFor(user -> {
            if (user.getUsername() != null) {
                return user.getUsername().length() >= 3;
            }
            return true;
        }, new BadRequestException("The username must be at least 3 characters long"));
    }
}
