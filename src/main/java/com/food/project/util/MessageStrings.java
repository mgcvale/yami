package com.food.project.util;

public enum MessageStrings {
    USER_EDIT_SUCCESS("Success editing user");

    private final String message;

    MessageStrings(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }
}
