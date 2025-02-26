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
    INVALID_ID("No user was found with this ID");
    private final String message;

    ErrorStrings(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
