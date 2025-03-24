package com.yamiapp.util;

import com.yamiapp.exception.ErrorStrings;
import com.yamiapp.exception.InvalidTokenException;
import com.yamiapp.exception.UnauthorizedException;

public class ControllerUtils {

    public static String extractToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException(ErrorStrings.INVALID_TOKEN.getMessage());
        }
        return authHeader.substring(7);
    }

}
