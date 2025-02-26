package com.food.project.util;

public enum MessageStrings {
    USER_CREATE_SUCCESS("Success creating user"),
    USER_AUTH_SUCCESS("Success editing user"),
    USER_EDIT_SUCCESS("Success editing user"),
    USER_DELETE_SUCCESS("Success deleting user");

    private final String message;

    MessageStrings(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
