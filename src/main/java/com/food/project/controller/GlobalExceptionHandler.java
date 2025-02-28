package com.food.project.controller;

import com.food.project.exception.*;
import com.food.project.util.ResponseFactory;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Internal exceptions
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException e) {
        logger.info("Handling BadRequestException: {}", e.getMessage());
        if (e.getMessage().equals(ErrorStrings.INVALID_USERNAME.getMessage())) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()), 401);
        }
        return ResponseFactory.createErrorResponse(e, 400);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleBadRequest(NotFoundException e) {
        logger.info("Handling NotFoundException: {}", e.getMessage());
        if (e.getMessage().equals(ErrorStrings.INVALID_USERNAME.getMessage())) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()), 401);
        }
        return ResponseFactory.createErrorResponse(e, 404);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException e) {
        logger.info("Handling ConflictException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(e, 409);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException e) {
        logger.info("Handling UnauthorizedException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(e, 401);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<Object> handleInternalServer(InternalServerException e) {
        logger.info("Handling InternalServerException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(e, 500);
    }

    // External exceptions
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException e) {
        logger.info("Handling EntityNotFoundException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new NotFoundException(ErrorStrings.NOT_FOUND.getMessage()), 404);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        logger.info("Handling DataIntegrityException: {}", e.getMessage());
        if (e.getCause() instanceof ConstraintViolationException) {
            return ResponseFactory.createErrorResponse(new ConflictException(ErrorStrings.CONFLICT.getMessage()), 409);
        }
        return ResponseFactory.createErrorResponse(new IntegrityViolationException(ErrorStrings.INTEGRITY.getMessage()), 409);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e) {
        logger.info("Handling ConstraintViolationException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new ConflictException(ErrorStrings.CONFLICT.getMessage()), 409);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception e) {
        logger.info("Handling GenericException: {}", e.getMessage());
        e.printStackTrace();
        return ResponseFactory.createErrorResponse(new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage()), 500);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(Exception e) {
        logger.info("Handling NoResourceFoundException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new NotFoundException(ErrorStrings.INVALID_PATH.getMessage()), 404);
    }
}
