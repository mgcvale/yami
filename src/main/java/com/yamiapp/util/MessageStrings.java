package com.yamiapp.util;

public enum MessageStrings {
    USER_CREATE_SUCCESS("Success creating user"),
    USER_AUTH_SUCCESS("Success editing user"),
    USER_EDIT_SUCCESS("Success editing user"),
    USER_DELETE_SUCCESS("Success deleting user"),
    RESTAURANT_CREATE_SUCCESS("Success creating restaurant"),
    RESTAURANT_UPDATE_SUCCESS("Success updating restaurant"),
    RESTAURANT_DELETE_SUCCESS("Success deleting restaurant");

    private final String message;

    MessageStrings(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
