package com.food.project.util;

import org.springframework.http.ResponseEntity;

import java.util.HashMap;
import java.util.Map;

public class ResponseFactory {

    public static class JsonResponseChain {
        private HashMap<String, Object> json;

         JsonResponseChain() {}

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

    public static ResponseEntity<Object> createErrorResponse(Exception e, int status) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "error");
        response.put("message", e.getMessage());
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Object> createSuccessResponse(String msg, int status) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("message", msg);
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Object> createSuccessResponse(int status) {
        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        return ResponseEntity.status(status).body(response);
    }

    public static ResponseEntity<Object> createSuccessResponse(String msg) {
        return createSuccessResponse(msg, 200);
    }

    public static ResponseEntity<Object> createSuccessResponse() {
        return createSuccessResponse(200);
    }


    public static JsonResponseChain createJsonResponse() {
        return new JsonResponseChain();
    }

}
