package com.food.project.exception;

public enum ErrorStrings {
    INVALID_TOKEN("Invalid access token"),
    INTERNAL_NO_RESULT("Database query returned no result"),
    CONFLICT_USERNAME("A user with this username already exists"),
    NO_USER_FOUND("No user was found"),
    INVALID_USERNAME_OR_PASSWORD("Invalid username or password"),
    INVALID_USERNAME("Invalid username"),
    INTERNAL_UNKNOWN("Unknown internal server error"),
    NOT_FOUND("Not found"),
    CONFLICT("Conflicting fields"),
    INTEGRITY("Integrity Violation"),
    INVALID_USER_ID("No user was found with this ID"),
    INVALID_FIELDS("One or more fields provided were invalid"),
    INVALID_EMAIL("The provided email is in an invalid format"),
    CONFLICT_EMAIL("A user with this email already exists"),
    SHORT_USERNAME("The username must be at least 3 characters long"),
    SHORT_PASSWORD("The password must be at least 8 characters long"),
    INVALID_PATH("One or more path arguments were missing, or the path was invalid altogether"),

    SHORT_RESTAURANT_NAME("The name of the restaurant must be at least 3 characters long"),
    RESTAURANT_IMAGE_UPSTREAM("Failed to upload restaurant picture due to backblaze upstream error"),
    CONFLICT_RESTAURANT_NAME("A Restaurant with this name already exists"),
    INVALID_RESTAURANT_ID("No restaurant was found with this ID"),

    INVALID_IMAGE_FILETYPE("The uploaded image is either in an unsupported format or not an image altogether."),
    FILE_TOO_LARGE("The file uploaded exceeded the maximum size of 5MB"),
    B2_FILE_NOT_FOUND("The file you requested wasn't found"),
    B2_FORBIDDEN_UNAUTHORIZED("You don't have permission to access this file."),
    B2_CONFLICT("There already exists a file with this name."),
    B2_INTERNAL("The Backblaze service is internally unavaliable"),
    B2_UPSTREAM("The Backblaze service is unavaliable."),
    INTERNAL_IO("An internal I/O error occourred."),

    FORBIDDEN_NOT_ADMIN("You must be an administrator to do this action.");


    private final String message;

    ErrorStrings(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
