package com.example.datn.service;

import com.example.datn.config.VNPayConfig;
import com.example.datn.dto.HoaDonDTO;
import com.example.datn.dto.HoaDonRequest;
import com.example.datn.dto.VNPayCallbackResponse;
import com.example.datn.dto.VNPayPaymentResponse;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.LichSuHoaDon;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.repository.LichSuHoaDonRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flow VNPay đúng:
 * 1. FE gọi POST /api/vnpay/create-payment với toàn bộ HoaDonRequest
 * 2. BE tạo đơn trạng thái CHO_THANH_TOAN → trả về paymentUrl
 * 3. FE redirect người dùng đến VNPay (CHƯA xóa giỏ hàng)
 * 4. VNPay callback -> GET /api/vnpay/payment-callback
 *    - Thành công (00): xác nhận đơn sang CHO_XAC_NHAN, FE xóa giỏ & hiển thị thành công
 *    - Thất bại/Hủy: hủy (xóa cứng) đơn CHO_THANH_TOAN, giỏ hàng giữ nguyên
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VNPayService {

    private final VNPayConfig vnPayConfig;
    private final HoaDonService hoaDonService;
    private final HoaDonRepository hoaDonRepository;
    private final LichSuHoaDonRepository lichSuHoaDonRepository;
    private final EmailService emailService;

    /**
     * Bước 1: Tạo đơn CHO_THANH_TOAN và sinh URL thanh toán VNPay.
     * HoaDonRequest phải chứa trangThai = "CHO_THANH_TOAN".
     */
    @Transactional
    public VNPayPaymentResponse createPaymentAndUrl(HttpServletRequest httpRequest, HoaDonRequest hoaDonRequest) {
        // Bắt buộc trạng thái = CHO_THANH_TOAN (đơn chờ thanh toán, chưa xác nhận)
        hoaDonRequest.setTrangThai("CHO_THANH_TOAN");

        // Tạo đơn hàng thực sự (tạm thời chờ thanh toán)
        HoaDonDTO hoaDon = hoaDonService.create(hoaDonRequest);
        log.info("Đã tạo đơn CHO_THANH_TOAN: {} (id={})", hoaDon.getMaHoaDon(), hoaDon.getId());

        // Lấy số tiền cần thanh toán
        BigDecimal soTien = hoaDon.getTienSauGiamGia();
        if (soTien == null || soTien.compareTo(BigDecimal.ZERO) <= 0) {
            // Hủy đơn vừa tạo nếu số tiền không hợp lệ
            hoaDonService.huyDonChoThanhToan(hoaDon.getId());
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
        }

        String paymentUrl = buildPaymentUrl(httpRequest, hoaDon.getMaHoaDon(), soTien);

        return VNPayPaymentResponse.builder()
            .status("OK")
            .message("Tạo liên kết thanh toán VNPay thành công")
            .paymentUrl(paymentUrl)
            .build();
    }

    /**
     * Bước 2: Xử lý callback từ VNPay sau khi người dùng thanh toán.
     * - vnp_ResponseCode = "00" → xác nhận đơn (CHO_THANH_TOAN -> CHO_XAC_NHAN)
     * - Còn lại → hủy (xóa cứng) đơn CHO_THANH_TOAN để giỏ hàng nguyên vẹn
     */
    @Transactional
    public VNPayCallbackResponse handlePaymentCallback(Map<String, String> fields) {
        String vnpSecureHash = fields.get("vnp_SecureHash");

        Map<String, String> fieldsCopy = new HashMap<>(fields);
        fieldsCopy.remove("vnp_SecureHashType");
        fieldsCopy.remove("vnp_SecureHash");

        String signValue = VNPayConfig.hashAllFields(fieldsCopy, vnPayConfig.getVnpHashSecret());

        // Tách mã hóa đơn từ vnp_TxnRef (format: MAHOADON_timestamp)
        String vnpTxnRef = fields.get("vnp_TxnRef");
        String maHoaDon = vnpTxnRef != null && vnpTxnRef.contains("_")
            ? vnpTxnRef.substring(0, vnpTxnRef.lastIndexOf("_"))
            : vnpTxnRef;

        String vnpResponseCode = fields.get("vnp_ResponseCode");
        String vnpTransactionNo = fields.get("vnp_TransactionNo");
        String vnpBankCode = fields.get("vnp_BankCode");
        String vnpAmountStr = fields.get("vnp_Amount");
        String vnpPayDate = fields.get("vnp_PayDate");

        BigDecimal amount = BigDecimal.ZERO;
        if (vnpAmountStr != null && !vnpAmountStr.trim().isEmpty()) {
            try {
                amount = new BigDecimal(vnpAmountStr).divide(BigDecimal.valueOf(100));
            } catch (Exception ignored) {}
        }

        // Tìm đơn hàng theo mã
        HoaDon hoaDon = null;
        if (maHoaDon != null && !maHoaDon.trim().isEmpty()) {
            hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon).orElse(null);
        }

        // Kiểm tra chữ ký bảo mật VNPay
        if (signValue == null || !signValue.equalsIgnoreCase(vnpSecureHash)) {
            log.warn("Chữ ký VNPay không hợp lệ! Kỳ vọng: {}, Nhận: {}", signValue, vnpSecureHash);
            // Không tự động hủy đơn khi chữ ký lỗi - có thể là tấn công giả mạo
            return VNPayCallbackResponse.builder()
                .success(false)
                .message("Chữ ký bảo mật không hợp lệ (Checksum failed). Giao dịch không được xử lý.")
                .orderId(hoaDon != null ? hoaDon.getId() : null)
                .maHoaDon(maHoaDon)
                .amount(amount)
                .bankCode(vnpBankCode)
                .transactionNo(vnpTransactionNo)
                .responseCode(vnpResponseCode)
                .payDate(vnpPayDate)
                .build();
        }

        boolean isSuccess = "00".equals(vnpResponseCode);
        String message = mapResponseCodeToMessage(vnpResponseCode);

        if (hoaDon == null) {
            log.error("Không tìm thấy đơn hàng với mã: {}", maHoaDon);
            return VNPayCallbackResponse.builder()
                .success(false)
                .message("Không tìm thấy đơn hàng " + maHoaDon + ". " + message)
                .maHoaDon(maHoaDon)
                .amount(amount)
                .bankCode(vnpBankCode)
                .transactionNo(vnpTransactionNo)
                .responseCode(vnpResponseCode)
                .payDate(vnpPayDate)
                .build();
        }

        if (isSuccess) {
            // ✅ Thanh toán thành công → xác nhận đơn hàng
            if ("CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
                hoaDonService.xacNhanThanhToanVNPay(hoaDon.getId());
                // Ghi ghi chú giao dịch vào đơn
                hoaDon = hoaDonRepository.findById(hoaDon.getId()).orElse(hoaDon);
                String vnpNote = String.format("[Thanh toán VNPAY | GD: %s | NH: %s | Số tiền: %,.0fđ]",
                    vnpTransactionNo, vnpBankCode, amount);
                String currentNote = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu().trim() : "";
                String updatedNote = currentNote.isEmpty() ? vnpNote : currentNote + " " + vnpNote;
                if (updatedNote.length() > 1000) updatedNote = updatedNote.substring(0, 1000);
                hoaDon.setGhiChu(updatedNote);
                hoaDon.setNgayCapNhat(LocalDateTime.now());
                hoaDonRepository.save(hoaDon);
                log.info("VNPay thành công → đơn {} chuyển sang CHO_XAC_NHAN (GD: {})", maHoaDon, vnpTransactionNo);

                // Gửi email xác nhận đơn hàng cho khách hàng
                try {
                    KhachHang kh = hoaDon.getKhachHang();
                    if (kh != null && kh.getEmail() != null && !kh.getEmail().isBlank()) {
                        String tenKH = kh.getTenKhachHang() != null ? kh.getTenKhachHang() : "Quý khách";
                        String soTienStr = String.format("%,.0f", amount);
                        String diaChi = hoaDon.getDiaChiNhanHang() != null ? hoaDon.getDiaChiNhanHang() : "Không có";
                        emailService.guiMailDatHangThanhCong(
                            kh.getEmail(), tenKH, maHoaDon, soTienStr, "VNPay QR", diaChi
                        );
                    }
                } catch (Exception ex) {
                    log.warn("Lỗi gửi email xác nhận đơn VNPay {}: {}", maHoaDon, ex.getMessage());
                }
            } else {
                // Idempotent: đã xử lý rồi
                log.info("VNPay callback đã xử lý trước đó cho đơn {}", maHoaDon);
            }
        } else {
            // ❌ Thanh toán thất bại hoặc hủy → xóa đơn CHO_THANH_TOAN để giỏ hàng nguyên vẹn
            if ("CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
                try {
                    hoaDonService.huyDonChoThanhToan(hoaDon.getId());
                    log.info("VNPay thất bại ({}) → đã xóa đơn tạm {} để giỏ hàng khôi phục", vnpResponseCode, maHoaDon);
                } catch (Exception e) {
                    log.error("Lỗi khi hủy đơn CHO_THANH_TOAN {}: {}", maHoaDon, e.getMessage());
                }
            }
        }

        return VNPayCallbackResponse.builder()
            .success(isSuccess)
            .message(message)
            .orderId(isSuccess && hoaDon != null ? hoaDon.getId() : null)
            .maHoaDon(isSuccess ? maHoaDon : null)
            .amount(amount)
            .bankCode(vnpBankCode)
            .transactionNo(vnpTransactionNo)
            .responseCode(vnpResponseCode)
            .payDate(vnpPayDate)
            .build();
    }

    // ===== Helpers =====

    private String buildPaymentUrl(HttpServletRequest request, String maHoaDon, BigDecimal soTien) {
        long amount = soTien.multiply(BigDecimal.valueOf(100)).longValue();
        // Thêm timestamp để đảm bảo vnp_TxnRef unique mỗi lần thử
        String vnpTxnRef = maHoaDon + "_" + System.currentTimeMillis();

        Map<String, String> vnpParams = new HashMap<>();
        vnpParams.put("vnp_Version", vnPayConfig.getVnpVersion());
        vnpParams.put("vnp_Command", vnPayConfig.getVnpCommand());
        vnpParams.put("vnp_TmnCode", vnPayConfig.getVnpTmnCode());
        vnpParams.put("vnp_Amount", String.valueOf(amount));
        vnpParams.put("vnp_CurrCode", "VND");
        vnpParams.put("vnp_TxnRef", vnpTxnRef);
        vnpParams.put("vnp_OrderInfo", "Thanh toan don hang " + maHoaDon);
        vnpParams.put("vnp_OrderType", "other");
        vnpParams.put("vnp_Locale", "vn");
        vnpParams.put("vnp_ReturnUrl", vnPayConfig.getVnpReturnUrl());
        vnpParams.put("vnp_IpAddr", VNPayConfig.getIpAddress(request));

        Calendar cld = Calendar.getInstance(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMddHHmmss");
        formatter.setTimeZone(TimeZone.getTimeZone("Asia/Ho_Chi_Minh"));
        vnpParams.put("vnp_CreateDate", formatter.format(cld.getTime()));
        cld.add(Calendar.MINUTE, 15);
        vnpParams.put("vnp_ExpireDate", formatter.format(cld.getTime()));

        List<String> fieldNames = new ArrayList<>(vnpParams.keySet());
        Collections.sort(fieldNames);
        StringBuilder hashData = new StringBuilder();
        StringBuilder query = new StringBuilder();
        Iterator<String> itr = fieldNames.iterator();
        while (itr.hasNext()) {
            String fieldName = itr.next();
            String fieldValue = vnpParams.get(fieldName);
            if (fieldValue != null && !fieldValue.trim().isEmpty()) {
                try {
                    String encValue = URLEncoder.encode(fieldValue, StandardCharsets.US_ASCII.toString());
                    String encName = URLEncoder.encode(fieldName, StandardCharsets.US_ASCII.toString());
                    hashData.append(fieldName).append('=').append(encValue);
                    query.append(encName).append('=').append(encValue);
                } catch (Exception e) {
                    log.error("Lỗi encode param VNPay: {}", e.getMessage());
                }
                if (itr.hasNext()) {
                    query.append('&');
                    hashData.append('&');
                }
            }
        }

        String secureHash = VNPayConfig.hmacSHA512(vnPayConfig.getVnpHashSecret(), hashData.toString());
        return vnPayConfig.getVnpPayUrl() + "?" + query + "&vnp_SecureHash=" + secureHash;
    }

    private String mapResponseCodeToMessage(String responseCode) {
        if (responseCode == null) return "Không có mã phản hồi";
        return switch (responseCode) {
            case "00" -> "Thanh toán thành công!";
            case "07" -> "Trừ tiền thành công. Giao dịch bị nghi ngờ gian lận.";
            case "09" -> "Thẻ/Tài khoản chưa đăng ký dịch vụ InternetBanking tại ngân hàng.";
            case "10" -> "Khách hàng xác thực thông tin không đúng quá 3 lần.";
            case "11" -> "Hết hạn chờ thanh toán. Vui lòng thực hiện lại giao dịch.";
            case "12" -> "Thẻ hoặc tài khoản đang bị khóa.";
            case "13" -> "Nhập sai mật khẩu OTP. Vui lòng thử lại.";
            case "24" -> "Bạn đã hủy giao dịch thanh toán.";
            case "51" -> "Tài khoản không đủ số dư để thực hiện giao dịch.";
            case "65" -> "Tài khoản đã vượt quá hạn mức giao dịch trong ngày.";
            case "75" -> "Ngân hàng đang bảo trì. Vui lòng thử lại sau.";
            case "79" -> "Nhập sai mật khẩu thanh toán quá số lần quy định.";
            default -> "Giao dịch không thành công (Mã lỗi: " + responseCode + ").";
        };
    }
}
