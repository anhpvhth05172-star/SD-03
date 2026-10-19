package com.example.datn.exception;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void responseStatusException401_Tra401() {
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.UNAUTHORIZED,
            "Chưa đăng nhập"
        );

        var resp = handler.handleResponseStatusException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(resp.getBody().getMessage()).contains("Chưa đăng nhập");
    }

    @Test
    void responseStatusException403_Tra403() {
        ResponseStatusException ex = new ResponseStatusException(
            HttpStatus.FORBIDDEN,
            "Cần đăng nhập nhân viên"
        );

        var resp = handler.handleResponseStatusException(ex);

        assertThat(resp.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(resp.getBody().getMessage()).contains("Cần đăng nhập nhân viên");
    }
}
