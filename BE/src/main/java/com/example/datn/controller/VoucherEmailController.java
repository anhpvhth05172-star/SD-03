package com.example.datn.controller;

import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.service.VoucherEmailService;
import tools.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/voucher-email")
@RequiredArgsConstructor
public class VoucherEmailController {

    private final VoucherEmailService voucherEmailService;
    private final ObjectMapper objectMapper;

    /**
     * Gửi email phiếu giảm giá cho 1 khách hàng
     * POST /api/voucher-email/send
     * Body:
     * {
     * "email": "khach@gmail.com",
     * "tenKhachHang": "Nguyễn Văn A",
     * "phieuGiamGia": { ... object PhieuGiamGia ... }
     * }
     */
    @PostMapping("/send")
    public ResponseEntity<?> sendVoucher(@RequestBody Map<String, Object> body) {
        try {
            String email = (String) body.get("email");
            String tenKhachHang = (String) body.get("tenKhachHang");
            PhieuGiamGia phieu = convertToPhieuGiamGia(body.get("phieuGiamGia"));

            if (email == null || email.isBlank()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Email không được để trống"
                ));
            }

            voucherEmailService.sendVoucher(email, tenKhachHang, phieu);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Gửi email thành công đến " + email
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Gửi email thất bại: " + e.getMessage()
            ));
        }
    }

    /**
     * Gửi email hàng loạt cho nhiều khách hàng
     * POST /api/voucher-email/send-batch
     * Body:
     * {
     * "phieuGiamGia": { ... },
     * "customers": [
     * { "email": "...", "tenKhachHang": "..." },
     * { "email": "...", "tenKhachHang": "..." }
     * ]
     * }
     */
    @PostMapping("/send-batch")
    public ResponseEntity<?> sendVoucherBatch(@RequestBody Map<String, Object> body) {
        try {
            PhieuGiamGia phieu = convertToPhieuGiamGia(body.get("phieuGiamGia"));
            List<Map<String, String>> customers = (List<Map<String, String>>) body.get("customers");

            if (customers == null || customers.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "message", "Danh sách khách hàng trống"
                ));
            }

            int success = 0;
            int fail = 0;

            for (Map<String, String> customer : customers) {
                try {
                    String email = customer.get("email");
                    String ten = customer.get("tenKhachHang");
                    if (email != null && !email.isBlank()) {
                        voucherEmailService.sendVoucher(email, ten, phieu);
                        success++;
                    }
                } catch (Exception e) {
                    fail++;
                }
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "Đã gửi thành công " + success + " email, thất bại " + fail
            ));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Gửi email hàng loạt thất bại: " + e.getMessage()
            ));
        }
    }

    // Helper: chuyển Object sang PhieuGiamGia
    private PhieuGiamGia convertToPhieuGiamGia(Object obj) {
        if (obj == null) {
            throw new IllegalArgumentException("Thiếu dữ liệu phiếu giảm giá");
        }
        if (obj instanceof PhieuGiamGia phieu) {
            return phieu;
        }
        if (obj instanceof Map<?, ?> map) {
            try {
                @SuppressWarnings("unchecked")
                Map<String, Object> m = (Map<String, Object>) map;
                PhieuGiamGia phieu = objectMapper.convertValue(m, PhieuGiamGia.class);
                phieu.setDotGiamGia(null); // tránh lazy-load không cần thiết khi gửi mail
                return phieu;
            } catch (RuntimeException e) {
                throw new IllegalArgumentException("Dữ liệu phiếu giảm giá không hợp lệ: " + e.getMessage());
            }
        }
        throw new IllegalArgumentException("Dữ liệu phiếu giảm giá không hợp lệ");
    }
}
