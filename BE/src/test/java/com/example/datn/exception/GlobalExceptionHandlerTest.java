package com.example.datn.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
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

    @Test
    void loiKhongXacNhiTra500VaGiauChiTiet() {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/api/nhan-vien/import/xac-nhan");

        ResponseEntity<ErrorResponse> res = handler.handleGeneralException(
            new RuntimeException("boom"), req);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, res.getStatusCode());
        ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals(500, body.getStatus());
        assertEquals("Đã xảy ra lỗi hệ thống. Vui lòng thử lại sau.", body.getMessage());
        assertNotNull(body.getErrors());
        assertEquals("boom", body.getErrors().get("chiTiet"));
    }

    @Test
    void messageNullVanTraChuoiKhongNull() {
        MockHttpServletRequest req = new MockHttpServletRequest("GET", "/api/nhan-vien");

        ResponseEntity<ErrorResponse> res = handler.handleGeneralException(
            new RuntimeException((String) null), req);

        ErrorResponse body = res.getBody();
        assertNotNull(body);
        assertEquals("null", body.getErrors().get("chiTiet"));
    }
}
