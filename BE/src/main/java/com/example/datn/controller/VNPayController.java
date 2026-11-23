package com.example.datn.controller;

import com.example.datn.dto.HoaDon.HoaDonRequest;
import com.example.datn.dto.VNPayCallbackResponse;
import com.example.datn.dto.VNPayPaymentResponse;
import com.example.datn.service.VNPayService;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
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
     * Tạo đơn hàng (CHO_THANH_TOAN) + URL thanh toán VNPay + Mã QR VietQR.
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
     * Tạo lại liên kết thanh toán VNPay cho đơn hàng đang chờ thanh toán.
     */
    @PostMapping("/retry-payment/{id}")
    public ResponseEntity<VNPayPaymentResponse> retryPayment(
        @PathVariable("id") Long id,
        @RequestParam(value = "idPhuongThucThanhToan", required = false) Long idPhuongThucThanhToan,
        HttpServletRequest httpRequest
    ) {
        return ResponseEntity.ok(vnPayService.retryPaymentAndUrl(httpRequest, id, idPhuongThucThanhToan));
    }

    /**
     * Xác nhận thanh toán khi quét mã QR thành công (tự động hoặc bấm xác nhận).
     * Chuyển đơn từ CHO_THANH_TOAN sang CHO_XAC_NHAN, trừ kho, xóa giỏ và gửi mail.
     */
    @PostMapping("/xac-nhan-qr/{id}")
    public ResponseEntity<VNPayCallbackResponse> xacNhanThanhToanQR(@PathVariable("id") Long id) {
        log.info("Khách hàng xác nhận quét mã QR cho đơn hàng #{}", id);
        VNPayCallbackResponse response = vnPayService.xacNhanThanhToanQR(id);
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

    /**
     * Kiểm tra trạng thái thanh toán của đơn hàng (FE polling để tự đóng modal QR khi SePay webhook xác nhận).
     */
    @PostMapping("/trang-thai/{id}")
    public ResponseEntity<Map<String, Object>> kiemTraTrangThai(@PathVariable("id") Long id) {
        return ResponseEntity.ok(vnPayService.kiemTraTrangThaiThanhToan(id));
    }

    /**
     * Kiểm tra trạng thái thanh toán theo nội dung chuyển khoản (POS trước khi tạo hóa đơn).
     */
    @PostMapping("/sepay-check-memo")
    public ResponseEntity<Map<String, Object>> kiemTraMemoSepay(@RequestBody Map<String, Object> body) {
        String memo = body != null && body.get("memo") != null ? String.valueOf(body.get("memo")) : "";
        BigDecimal amount = BigDecimal.ZERO;
        if (body != null && body.get("amount") != null) {
            try {
                amount = new BigDecimal(String.valueOf(body.get("amount")));
            } catch (NumberFormatException ignored) {}
        }
        boolean consume = body != null && Boolean.TRUE.equals(body.get("consume"));
        return ResponseEntity.ok(vnPayService.kiemTraMemoSepay(memo, amount, consume));
    }

    /**
     * Webhook nhận thông báo biến động số dư từ SePay.
     */
    @PostMapping("/sepay-webhook")
    public ResponseEntity<Map<String, Object>> sepayWebhook(
        @RequestBody(required = false) Map<String, Object> payload,
        @RequestHeader(value = "Authorization", required = false) String authHeader
    ) {
        try {
            return ResponseEntity.ok(vnPayService.xuLyWebhookSepay(payload, authHeader));
        } catch (SecurityException ex) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("success", false, "message", ex.getMessage()));
        }
    }
}
