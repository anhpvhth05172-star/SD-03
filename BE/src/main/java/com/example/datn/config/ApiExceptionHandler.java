package com.example.datn.config;

import java.util.HashMap;
import java.util.Map;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Dữ liệu không hợp lệ";
        return ResponseEntity.badRequest().body(message(message));
    }

//    @ExceptionHandler(DataIntegrityViolationException.class)
//    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
//        return ResponseEntity.badRequest().body(message("Dữ liệu vi phạm ràng buộc dữ liệu (duy nhất/khóa ngoại)"));
//    }

    private static Map<String, String> message(String text) {
        Map<String, String> body = new HashMap<>();
        body.put("message", text);
        return body;
    }
}
