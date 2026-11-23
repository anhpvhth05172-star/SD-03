package com.example.datn.config;

import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.exception.KhongCoQuyenException;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> handleIllegalArgument(IllegalArgumentException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Dữ liệu không hợp lệ";
        return ResponseEntity.badRequest().body(message(message));
    }

    @ExceptionHandler(DangNhapThatBaiException.class)
    public ResponseEntity<Map<String, String>> handleDangNhapThatBai(DangNhapThatBaiException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Sai tài khoản hoặc mật khẩu";
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(message(message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, String>> handleNotReadable(HttpMessageNotReadableException ex) {
        return ResponseEntity.badRequest()
            .body(message("Dữ liệu gửi lên không hợp lệ (JSON không đúng định dạng)"));
    }

    @ExceptionHandler(KhongCoQuyenException.class)
    public ResponseEntity<Map<String, String>> handleKhongCoQuyen(KhongCoQuyenException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(message(ex.getMessage()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, String>> handleDataIntegrity(DataIntegrityViolationException ex) {
        Throwable root = ex.getMostSpecificCause();
        String detail = root != null && root.getMessage() != null ? root.getMessage().trim() : "";
        log.warn("Vi phạm ràng buộc dữ liệu: {}", detail, ex);
        String msg = detail.isEmpty()
                ? "Dữ liệu vi phạm ràng buộc dữ liệu (duy nhất/khóa ngoại)"
                : "Dữ liệu vi phạm ràng buộc dữ liệu: " + detail;
        return ResponseEntity.badRequest().body(message(msg));
    }

    private static Map<String, String> message(String text) {
        Map<String, String> body = new HashMap<>();
        body.put("message", text);
        return body;
    }
}
