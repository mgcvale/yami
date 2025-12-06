package com.yamiapp.util;

import lombok.Getter;

@Getter
public enum MessageStrings {
    USER_CREATE_SUCCESS("Success creating user"),
    USER_AUTH_SUCCESS("Success authenticating user"),
    USER_EDIT_SUCCESS("Success editing user"),
    USER_DELETE_SUCCESS("Success deleting user"),
    RESTAURANT_CREATE_SUCCESS("Success creating restaurant"),
    RESTAURANT_UPDATE_SUCCESS("Success updating restaurant"),
    RESTAURANT_DELETE_SUCCESS("Success deleting restaurant"),
    FOOD_CREATE_SUCCESS("Success creating food"),
    FOOD_DELETE_SUCCESS("Success deleting food"),
    FOOD_UPDATE_SUCCESS("Success updating food"),
    FOOD_REVIEW_DELETE_SUCCESS("Success deleting food"),
    FOLLOW_DELETE_SUCCESS("Success unfollowing user"),
    FOLLOW_CREATE_SUCCESS("Success following user"),
    RECOVERY_REQUEST_SUCCESS("Success sending password recovery email"),
    RECOVERY_SUCCESS("Success updating password");



    private final String message;

    MessageStrings(String message) {
        this.message = message;
    }

}
