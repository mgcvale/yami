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
    INVALID_ID("No user was found with this ID"),
    INVALID_FIELDS("One or more fields provided were invalid"),
    INVALID_EMAIL("The provided email is in an invalid format"),
    CONFLICT_EMAIL("A user with this email already exists"),
    SHORT_USERNAME("The username must be at least 3 characters long"),
    SHORT_PASSWORD("The password must be at least 8 characters long"),
    INVALID_PATH("One or more path arguments were missing, or the path was invalid altogether");
    private final String message;

    ErrorStrings(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
