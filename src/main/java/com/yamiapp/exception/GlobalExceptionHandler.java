package com.yamiapp.exception;

import com.yamiapp.util.ResponseFactory;
import jakarta.persistence.EntityNotFoundException;
import org.hibernate.exception.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    // Internal exceptions
    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<Object> handleBadRequest(BadRequestException e) {
        logger.info("Handling BadRequestException: {}", e.getMessage());
        if (e.getMessage().equals(ErrorStrings.INVALID_USERNAME.getMessage())) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()), HttpStatus.UNAUTHORIZED.value());
        }
        return ResponseFactory.createErrorResponse(e, HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<Object> handleBadRequest(NotFoundException e) {
        logger.info("Handling NotFoundException: {}", e.getMessage());
        if (e.getMessage().equals(ErrorStrings.INVALID_USERNAME.getMessage())) {
            return ResponseFactory.createErrorResponse(new UnauthorizedException(ErrorStrings.INVALID_USERNAME_OR_PASSWORD.getMessage()), HttpStatus.UNAUTHORIZED.value());
        }
        return ResponseFactory.createErrorResponse(e, HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<Object> handleConflict(ConflictException e) {
        logger.info("Handling ConflictException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(e, HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<Object> handleUnauthorized(UnauthorizedException e) {
        logger.info("Handling UnauthorizedException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(e, HttpStatus.UNAUTHORIZED.value());
    }

    @ExceptionHandler(InternalServerException.class)
    public ResponseEntity<Object> handleInternalServer(InternalServerException e) {
        logger.info("Handling InternalServerException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(e, HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<Object> handleForbidden(ForbiddenException e) {
        logger.info("Handling ForbiddenException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(e, HttpStatus.FORBIDDEN.value());
    }

    @ExceptionHandler(BadGatewayException.class)
    public ResponseEntity<Object> handleBadGateway(BadGatewayException e) {
        logger.info("Handling BadGatewayException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(e, HttpStatus.BAD_GATEWAY.value());
    }

    // External exceptions
    @ExceptionHandler(EntityNotFoundException.class)
    public ResponseEntity<Object> handleEntityNotFound(EntityNotFoundException e) {
        logger.info("Handling EntityNotFoundException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new NotFoundException(ErrorStrings.NOT_FOUND.getMessage()), HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Object> handleDataIntegrityViolation(DataIntegrityViolationException e) {
        logger.info("Handling DataIntegrityException: {}", e.getMessage());
        e.printStackTrace();
        if (e.getCause() instanceof ConstraintViolationException) {
            return ResponseFactory.createErrorResponse(new ConflictException(ErrorStrings.CONFLICT.getMessage()), HttpStatus.CONFLICT.value());
        }
        return ResponseFactory.createErrorResponse(new IntegrityViolationException(ErrorStrings.INTEGRITY.getMessage()), HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolation(ConstraintViolationException e) {
        logger.info("Handling ConstraintViolationException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new ConflictException(ErrorStrings.CONFLICT.getMessage()), HttpStatus.CONFLICT.value());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Object> handleGenericException(Exception e) {
        logger.info("Handling GenericException: {}", e.getMessage());
        e.printStackTrace();
        return ResponseFactory.createErrorResponse(new InternalServerException(ErrorStrings.INTERNAL_UNKNOWN.getMessage()), HttpStatus.INTERNAL_SERVER_ERROR.value());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Object> handleNoResourceFound(NoResourceFoundException e) {
        logger.info("Handling NoResourceFoundException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new NotFoundException(ErrorStrings.INVALID_PATH.getMessage()), HttpStatus.NOT_FOUND.value());
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<Object> handleMaxUploadSizeExceeded(MaxUploadSizeExceededException e) {
        logger.info("Handling MaxUploadSizeExceededException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new PayloadTooLargeException(ErrorStrings.FILE_TOO_LARGE.getMessage()), HttpStatus.PAYLOAD_TOO_LARGE.value());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException e) {
        logger.info("Handling HttpMessageNotReadableException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new BadRequestException(ErrorStrings.EMPTY_FIELDS.getMessage()), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Object> handleMissingServletRequestParameter(MissingServletRequestParameterException e) {
        logger.info("Handling MissingServletRequestParameterException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new BadRequestException(ErrorStrings.EMPTY_FIELDS.getMessage()), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(MissingServletRequestPartException.class)
    public ResponseEntity<Object> handleMissingServletRequestPart(MissingServletRequestParameterException e) {
        logger.info("Handling MissingServletRequestPartException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new BadRequestException(ErrorStrings.EMPTY_FIELDS.getMessage()), HttpStatus.BAD_REQUEST.value());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Object> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        logger.info("Handling HttpRequestMethodNotSupportedException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new MethodNotAllowedException(ErrorStrings.METHOD_NOT_ALLOWED.getMessage()), HttpStatus.METHOD_NOT_ALLOWED.value());
    }

    @ExceptionHandler(InvalidTokenException.class)
    public ResponseEntity<Object> handleInvalidToken(InvalidTokenException e) {
        logger.info("Handling InvalidTokenException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new UnauthorizedException(e.getMessage()), HttpStatus.UNAUTHORIZED.value());
    }

    @ExceptionHandler(HttpMediaTypeException.class)
    public ResponseEntity<Object> handleUnsupportedMediaType(HttpMediaTypeException e) {
        logger.info("Handling HttpMediaTypeException: {}", e.getMessage());
        return ResponseFactory.createErrorResponse(new UnsupportedMediaTypeException(e.getMessage()), HttpStatus.UNSUPPORTED_MEDIA_TYPE.value());
    }
}
