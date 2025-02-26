package com.food.project.validator;

import com.food.project.exception.BadRequestException;
import com.food.project.model.dto.UserDTO;

public class UserCreateRequestValidator extends Validator<UserDTO> {

    protected void initializeValidations() {
        ruleFor(user -> user.getLocation() != null, new BadRequestException("The location can't be null"));
        ruleFor(user -> user.getUsername() != null, new BadRequestException("The username can't be null"));
        ruleFor(user -> user.getBio() != null, new BadRequestException("The bio can't be null"));
        ruleFor(user -> user.getPassword() != null, new BadRequestException("The password can't be null"));
        ruleFor(user -> user.getEmail() != null, new BadRequestException("The email can't be null"));
        ruleFor(user -> user.getEmail().matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"), new BadRequestException("The email is in an invalid format."));

        ruleFor(user -> {
            if (user.getPassword() != null) {
                return user.getPassword().length() >= 8;
            }
            return false;
        }, new BadRequestException("The password must be at least 8 characters long"));
        ruleFor(user -> {
            if (user.getUsername() != null) {
                return user.getUsername().length() >= 3;
            }
            return false;
        }, new BadRequestException("The username must be at least 3 characters long"));
    }

}
