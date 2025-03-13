package com.yamiapp.validator;

import com.yamiapp.exception.BadRequestException;
import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.model.dto.UserLoginDTO;
import org.springframework.stereotype.Component;

@Component
public class UserLoginRequestValidator extends Validator<UserLoginDTO> {

    public UserLoginRequestValidator() {
        super();
    }

    @Override
    protected void initializeValidations() {
        ruleFor(u -> u.getPassword() != null, new BadRequestException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));
        ruleFor(u -> {
            if (u.getUsername() == null && u.getEmail() == null) {
                return false;
            }
            if (u.getUsername() == null) u.setUsername("");
            if (u.getEmail() == null) u.setEmail("");
            return true;
        }, new BadRequestException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()));
    }
}
