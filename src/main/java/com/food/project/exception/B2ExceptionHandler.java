package com.food.project.exception;

import com.backblaze.b2.client.exceptions.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class B2ExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @ExceptionHandler(B2NotFoundException.class)
    public void handleB2NotFound(B2NotFoundException e) {
        logger.info("Pre-handling B2NotFoundException: {}", e.getMessage());
        throw new NotFoundException(ErrorStrings.B2_FILE_NOT_FOUND.getMessage());
    }

    @ExceptionHandler(B2UnauthorizedException.class)
    public void handleB2Unauthorized(B2UnauthorizedException e) {
        logger.info("Pre-handling B2UnauthorizedException: {}", e.getMessage());
        throw new UnauthorizedException(ErrorStrings.B2_FORBIDDEN_UNAUTHORIZED.getMessage());
    }

    @ExceptionHandler(B2ForbiddenException.class)
    public void handleB2Forbidden(B2ForbiddenException e) {
        logger.info("Pre-handling B2ForbiddenException: {}", e.getMessage());
        throw new ForbiddenException(ErrorStrings.B2_FORBIDDEN_UNAUTHORIZED.getMessage());
    }

    @ExceptionHandler(B2NetworkException.class)
    public void handleB2Network(B2NetworkException e) {
        logger.info("Pre-handling B2NetworkException: {}", e.getMessage());
        throw new BadGatewayException(ErrorStrings.B2_UPSTREAM.getMessage());
    }

    @ExceptionHandler(B2LocalException.class)
    public void handleB2Local(B2LocalException e) {
        logger.info("Pre-handling B2LocalException: {}", e.getMessage());
        throw new InternalServerException(ErrorStrings.B2_INTERNAL.getMessage());
    }

    @ExceptionHandler(B2Exception.class)
    public void handleB2(B2Exception e) {
        logger.info("Pre-handling B2Exception: {}", e.getMessage());
        throw new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage());
    }
}
