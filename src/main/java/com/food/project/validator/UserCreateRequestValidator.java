package com.food.project.validator;

import com.food.project.exception.BadRequestException;
import com.food.project.exception.ErrorStrings;
import com.food.project.model.dto.UserDTO;

public class UserCreateRequestValidator extends Validator<UserDTO> {

    public UserCreateRequestValidator() {
        super();
    }

    protected void initializeValidations() {
        ruleFor(user -> user.getLocation() != null, new BadRequestException(ErrorStrings.INVALID_FIELDS.getMessage()));
        ruleFor(user -> user.getUsername() != null, new BadRequestException(ErrorStrings.INVALID_FIELDS.getMessage()));
        ruleFor(user -> user.getBio() != null, new BadRequestException(ErrorStrings.INVALID_FIELDS.getMessage()));
        ruleFor(user -> user.getPassword() != null, new BadRequestException(ErrorStrings.INVALID_FIELDS.getMessage()));
        ruleFor(user -> user.getEmail() != null, new BadRequestException(ErrorStrings.INVALID_FIELDS.getMessage()));
        ruleFor(user -> user.getEmail().matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x20\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])"), new BadRequestException(ErrorStrings.INVALID_EMAIL.getMessage()));

        ruleFor(user -> {
            if (user.getPassword() != null) {
                return user.getPassword().length() >= 8;
            }
            return false;
        }, new BadRequestException(ErrorStrings.SHORT_PASSWORD.getMessage()));
        ruleFor(user -> {
            if (user.getUsername() != null) {
                return user.getUsername().length() >= 3;
            }
            return false;
        }, new BadRequestException(ErrorStrings.SHORT_USERNAME.getMessage()));
    }

}
