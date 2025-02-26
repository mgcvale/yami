package com.food.project.validator;

import com.food.project.exception.BadRequestException;
import com.food.project.model.dto.UserDTO;

public class UserCreateRequestValidator extends Validator<UserDTO> {

    protected void initializeValidations() {
        ruleFor(user -> user.getLocation() != null, new BadRequestException("The location can't be null"));
        ruleFor(user -> user.getUsername() != null, new BadRequestException("The username can't be null"));
        ruleFor(user -> user.getBio() != null, new BadRequestException("The bio can't be null"));
        ruleFor(user -> user.getPassword() != null, new BadRequestException("The password can't be null"));

        ruleFor(user -> {
            if (user.getPassword() != null) {
                return user.getPassword().length() >= 8;
            }
            return false;
        }, new BadRequestException("The password must be at lesat 8 characters long"));
        ruleFor(user -> {
            if (user.getUsername() != null) {
                return user.getUsername().length() >= 3;
            }
            return false;
        }, new BadRequestException("The username must be at least 3 characters long"));
    }

}
