package com.yamiapp.util;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

public class ResponseFactory {

    public static class JsonResponseChain {
        private Map<String, Object> json;

        public JsonResponseChain() {
            json = new LinkedHashMap<>();
        }

        public JsonResponseChain add(String key, Object value) {
            json.put(key, value);
            return this;
        }

        public ResponseEntity<Object> build(int code) {
            return ResponseEntity.status(code).body(json);
        }

        public ResponseEntity<Object> build() {
            return ResponseEntity.ok().body(json);
        }
    }

    public static ResponseEntity<Map<String, String>> createErrorResponse(Exception e, int status) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", e.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Map<String, String>> createSuccessResponse(String msg, int status) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", msg);
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Map<String, String>> createSuccessResponse(int status) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Map<String, String>> createSuccessResponse(String msg) {
        return createSuccessResponse(msg, 200);
    }

    public static ResponseEntity<Map<String, String>> createSuccessResponse() {
        return createSuccessResponse(200);
    }

    public static JsonResponseChain createJsonResponse() {
        return new JsonResponseChain();
    }

}
