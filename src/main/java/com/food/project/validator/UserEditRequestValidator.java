package com.food.project.validator;

import com.food.project.exception.BadRequestException;
import com.food.project.exception.ErrorStrings;
import com.food.project.model.dto.UserDTO;

public class UserEditRequestValidator extends Validator<UserDTO> {

    public UserEditRequestValidator() {
        super();
    }

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

        ruleFor(user -> {
            if (user.getEmail() == null) {
                return true;
            }
            return user.getEmail().matches("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x20\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");
        }, new BadRequestException(ErrorStrings.INVALID_EMAIL.getMessage()));
    }
}
