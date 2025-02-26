package com.food.project.controller;

import com.food.project.exception.*;
import com.food.project.util.ResponseFactory;
import jakarta.persistence.EntityNotFoundException;
import org.apache.coyote.Response;
import org.hibernate.annotations.NotFound;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {

    // Internal exceptions
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException e) {
        if (e.getMessage().equals(ErrorStrings.INVALID_USERNAME.getMessage())) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()), 401);
        }
        return ResponseFactory.createErrorResponse(e, 400);
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleBadRequest(NotFoundException e) {
        if (e.getMessage().equals(ErrorStrings.INVALID_USERNAME.getMessage())) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()), 401);
        }
        return ResponseFactory.createErrorResponse(e, 404);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException e) {
        return ResponseFactory.createErrorResponse(e, 409);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException e) {
        return ResponseFactory.createErrorResponse(e, 401);
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<Object> handleInternalServer(InternalServerException e) {
        return ResponseFactory.createErrorResponse(e, 500);
    }

    // External exceptions
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException e) {
        return ResponseFactory.createErrorResponse(new NotFoundException(ErrorStrings.NOT_FOUND.getMessage()), 404);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        if (e.getCause() instanceof ConstraintViolationException) {
            return ResponseFactory.createErrorResponse(new ConflictException(ErrorStrings.CONFLICT.getMessage()), 409);
        }
        return ResponseFactory.createErrorResponse(new IntegrityViolationException(ErrorStrings.INTEGRITY.getMessage()), 409);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e) {
        return ResponseFactory.createErrorResponse(new ConflictException(ErrorStrings.CONFLICT.getMessage()), 409);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception e) {
        return ResponseFactory.createErrorResponse(new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage()), 500);
    }
}
