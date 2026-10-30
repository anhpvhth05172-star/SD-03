package com.example.datn.controller;

import com.example.datn.dto.HoaDonRequest;
import com.example.datn.dto.VNPayCallbackResponse;
import com.example.datn.dto.VNPayPaymentResponse;
import com.example.datn.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/vnpay")
@RequiredArgsConstructor
public class VNPayController {

    private final VNPayService vnPayService;

    /**
     * Tạo đơn hàng (CHO_THANH_TOAN) + URL thanh toán VNPay.
     * Body: HoaDonRequest đầy đủ (tương tự POST /api/hoa-don nhưng cho VNPay).
     */
    @PostMapping("/create-payment")
    public ResponseEntity<VNPayPaymentResponse> createPayment(
        @RequestBody HoaDonRequest request,
        HttpServletRequest httpRequest
    ) {
        VNPayPaymentResponse response = vnPayService.createPaymentAndUrl(httpRequest, request);
        return ResponseEntity.ok(response);
    }

    /**
     * Nhận kết quả thanh toán sau khi VNPay redirect về (FE gọi lại BE để xác thực).
     * Thành công → đơn CHO_THANH_TOAN chuyển sang CHO_XAC_NHAN.
     * Thất bại/Hủy → đơn CHO_THANH_TOAN bị xóa, giỏ hàng giữ nguyên.
     */
    @GetMapping("/payment-callback")
    public ResponseEntity<VNPayCallbackResponse> paymentCallback(
        @RequestParam Map<String, String> params
    ) {
        log.info("Nhận callback từ VNPay: vnp_ResponseCode={}, vnp_TxnRef={}",
            params.get("vnp_ResponseCode"), params.get("vnp_TxnRef"));
        VNPayCallbackResponse response = vnPayService.handlePaymentCallback(params);
        return ResponseEntity.ok(response);
    }

    /**
     * VNPay IPN (Server-to-Server notification) – backup xác nhận phía server.
     */
    @GetMapping("/vnpay-ipn")
    public ResponseEntity<Map<String, String>> vnpayIpn(
        @RequestParam Map<String, String> params
    ) {
        log.info("Nhận IPN từ VNPay: {}", params);
        VNPayCallbackResponse res = vnPayService.handlePaymentCallback(params);
        if ("Chữ ký bảo mật không hợp lệ (Checksum failed). Giao dịch không được xử lý.".equals(res.getMessage())) {
            return ResponseEntity.ok(Map.of("RspCode", "97", "Message", "Invalid Checksum"));
        }
        if (res.getOrderId() == null && !res.isSuccess()) {
            return ResponseEntity.ok(Map.of("RspCode", "01", "Message", "Order not Found"));
        }
        return ResponseEntity.ok(Map.of("RspCode", "00", "Message", "Confirm Success"));
    }
}
