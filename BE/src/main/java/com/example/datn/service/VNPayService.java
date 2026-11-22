package com.example.datn.service;

import com.example.datn.config.VNPayConfig;
import com.example.datn.dto.HoaDon.HoaDonDTO;
import com.example.datn.dto.HoaDon.HoaDonRequest;
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
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${sepay.webhook.api-key:}")
    private String sepayWebhookApiKey;

    private static final Pattern MA_HOA_DON_PATTERN = Pattern.compile("(?i)(HD\\s*[A-Za-z0-9_-]+)");
    private final Map<String, BigDecimal> sepayPendingMemoAmounts = new ConcurrentHashMap<>();
    private final Map<String, Long> activeListeningMemos = new ConcurrentHashMap<>();

    /**
     * Bước 1: Tạo đơn CHO_THANH_TOAN và sinh URL thanh toán VNPay.
     * HoaDonRequest phải chứa trangThai = "CHO_THANH_TOAN".
     */
    @Transactional
    public VNPayPaymentResponse createPaymentAndUrl(HttpServletRequest httpRequest, HoaDonRequest hoaDonRequest) {
        hoaDonRequest.setTrangThai("CHO_THANH_TOAN");

        HoaDonDTO hoaDon = hoaDonService.create(hoaDonRequest);
        log.info("Đã tạo đơn CHO_THANH_TOAN: {} (id={})", hoaDon.getMaHoaDon(), hoaDon.getId());

        BigDecimal soTien = hoaDon.getTienSauGiamGia();
        if (soTien == null || soTien.compareTo(BigDecimal.ZERO) <= 0) {
            hoaDonService.huyDonChoThanhToan(hoaDon.getId());
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
        }

        String paymentUrl = buildPaymentUrl(httpRequest, hoaDon.getMaHoaDon(), soTien);
        String qrUrl = buildQrUrl(hoaDon.getMaHoaDon(), soTien);

        return VNPayPaymentResponse.builder()
            .status("OK")
            .message("Tạo liên kết thanh toán VNPay thành công")
            .paymentUrl(paymentUrl)
            .orderId(hoaDon.getId())
            .maHoaDon(hoaDon.getMaHoaDon())
            .amount(soTien)
            .qrUrl(qrUrl)
            .build();
    }

    /** Tạo lại liên kết thanh toán VNPay cho đơn đang ở trạng thái CHO_THANH_TOAN. */
    @Transactional
    public VNPayPaymentResponse retryPaymentAndUrl(HttpServletRequest httpRequest, Long id) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
            .filter(h -> !h.isDaXoa())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));
        if (!"CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
            throw new IllegalArgumentException("Chỉ có thể thanh toán lại đơn hàng đang ở trạng thái Chờ thanh toán");
        }
        BigDecimal soTien = hoaDon.getTienSauGiamGia();
        if (soTien == null || soTien.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Số tiền thanh toán phải lớn hơn 0");
        }
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepository.save(hoaDon);

        String paymentUrl = buildPaymentUrl(httpRequest, hoaDon.getMaHoaDon(), soTien);
        String qrUrl = buildQrUrl(hoaDon.getMaHoaDon(), soTien);

        return VNPayPaymentResponse.builder()
            .status("OK")
            .message("Tạo lại liên kết thanh toán VNPay thành công")
            .paymentUrl(paymentUrl)
            .orderId(hoaDon.getId())
            .maHoaDon(hoaDon.getMaHoaDon())
            .amount(soTien)
            .qrUrl(qrUrl)
            .build();
    }

    private String buildQrUrl(String maHoaDon, BigDecimal soTien) {
        return String.format("https://img.vietqr.io/image/MB-0922010408-compact2.png?amount=%.0f&addInfo=%s&accountName=%s",
            soTien.doubleValue(),
            URLEncoder.encode(maHoaDon, StandardCharsets.UTF_8),
            URLEncoder.encode("POLYSHOES STORE", StandardCharsets.UTF_8));
    }

    @Transactional
    public VNPayCallbackResponse xacNhanThanhToanQR(Long id) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));

        if (!"CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
            return VNPayCallbackResponse.builder()
                .success(true)
                .orderId(hoaDon.getId())
                .maHoaDon(hoaDon.getMaHoaDon())
                .amount(hoaDon.getTienSauGiamGia())
                .message("Đơn hàng đã được xác nhận thanh toán trước đó.")
                .build();
        }

        HoaDonDTO dto = hoaDonService.xacNhanThanhToanVNPay(hoaDon.getId());
        hoaDon = hoaDonRepository.findById(hoaDon.getId()).orElse(hoaDon);

        String vnpNote = String.format("[Thanh toán quét mã QR thành công | Số tiền: %,.0fđ]", hoaDon.getTienSauGiamGia());
        String currentNote = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu().trim() : "";
        String updatedNote = currentNote.isEmpty() ? vnpNote : currentNote + " " + vnpNote;
        if (updatedNote.length() > 1000) updatedNote = updatedNote.substring(0, 1000);
        hoaDon.setGhiChu(updatedNote);
        hoaDon.setNgayCapNhat(LocalDateTime.now());
        hoaDonRepository.save(hoaDon);

        boolean outOfStock = hoaDon.getGhiChu() != null && hoaDon.getGhiChu().contains("CẢNH BÁO TỒN KHO");
        String msg = outOfStock
            ? "Thanh toán thành công, nhưng một số sản phẩm tạm hết hàng. Đơn hàng #" + hoaDon.getMaHoaDon() + " đã chuyển sang Chờ xác nhận để nhân viên liên hệ."
            : "Xác nhận thanh toán thành công! Đơn hàng #" + hoaDon.getMaHoaDon() + " đang ở trạng thái Chờ xác nhận.";

        return VNPayCallbackResponse.builder()
            .success(true)
            .outOfStock(outOfStock)
            .orderId(hoaDon.getId())
            .maHoaDon(hoaDon.getMaHoaDon())
            .amount(hoaDon.getTienSauGiamGia())
            .message(msg)
            .build();
    }

    @Transactional
    public VNPayCallbackResponse handlePaymentCallback(Map<String, String> fields) {
        String vnpSecureHash = fields.get("vnp_SecureHash");

        Map<String, String> fieldsCopy = new HashMap<>(fields);
        fieldsCopy.remove("vnp_SecureHashType");
        fieldsCopy.remove("vnp_SecureHash");

        String signValue = VNPayConfig.hashAllFields(fieldsCopy, vnPayConfig.getVnpHashSecret());

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

        HoaDon hoaDon = null;
        if (maHoaDon != null && !maHoaDon.trim().isEmpty()) {
            hoaDon = hoaDonRepository.findByMaHoaDon(maHoaDon).orElse(null);
        }

        if (signValue == null || !signValue.equalsIgnoreCase(vnpSecureHash)) {
            log.warn("Chữ ký VNPay không hợp lệ! Kỳ vọng: {}, Nhận: {}", signValue, vnpSecureHash);
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
            if ("CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
                HoaDonDTO dto = hoaDonService.xacNhanThanhToanVNPay(hoaDon.getId());
                // Ghi ghi chú giao dịch vào đơn
                hoaDon = hoaDonRepository.findById(hoaDon.getId()).orElse(hoaDon);
                String vnpNote = String.format("[Thanh toán VNPAY | GD: %s | NH: %s | Số tiền: %,.0fđ]",
                    vnpTransactionNo, vnpBankCode, amount);
                String currentNote = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu().trim() : "";
                String updatedNote = currentNote.isEmpty() ? vnpNote : currentNote + " " + vnpNote;
                if (updatedNote.length() > 1000) updatedNote = updatedNote.substring(0, 1000);
//                hoaDon.setGhiChu(updatedNote);
                hoaDon.setNgayCapNhat(LocalDateTime.now());
                hoaDonRepository.save(hoaDon);

                log.info("VNPay thành công → đơn {} chuyển sang CHO_XAC_NHAN (GD: {})", maHoaDon, vnpTransactionNo);
            } else {
                log.info("VNPay callback đã xử lý trước đó cho đơn {}", maHoaDon);
            }
        } else {
            if ("CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
                try {
                    // Không hủy đơn ngay: Treo đơn hàng 5 phút để khách có thể thanh toán lại
                    hoaDon.setNgayCapNhat(LocalDateTime.now());
                    String failNote = String.format("[Thanh toán chưa hoàn tất (Lỗi: %s) - Treo đơn 5 phút để thanh toán lại]", vnpResponseCode);
                    String currentNote = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu().trim() : "";
                    if (!currentNote.contains("Treo đơn 5 phút")) {
                        String updated = currentNote.isEmpty() ? failNote : currentNote + " " + failNote;
                        if (updated.length() > 1000) updated = updated.substring(0, 1000);
                        hoaDon.setGhiChu(updated);
                    }
                    hoaDonRepository.save(hoaDon);
                    log.info("VNPay chưa thành công ({}) → Treo đơn tạm {} trong 5 phút thay vì hủy luôn", vnpResponseCode, maHoaDon);
                } catch (Exception e) {
                    log.error("Lỗi khi cập nhật ghi chú treo đơn {}: {}", maHoaDon, e.getMessage());
                }
            }
        }

        return VNPayCallbackResponse.builder()
            .success(isSuccess)
            .message(isSuccess ? message : message + " Đơn hàng của bạn sẽ được treo giữ chỗ trong 5 phút để bạn có thể thanh toán lại.")
            .orderId(hoaDon != null ? hoaDon.getId() : null)
            .maHoaDon(maHoaDon)
            .amount(amount)
            .bankCode(vnpBankCode)
            .transactionNo(vnpTransactionNo)
            .responseCode(vnpResponseCode)
            .payDate(vnpPayDate)
            .build();
    }


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

    @Transactional
    public Map<String, Object> kiemTraTrangThaiThanhToan(Long id) {
        HoaDon hoaDon = hoaDonRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đơn hàng #" + id));
        boolean paid = !"CHO_THANH_TOAN".equals(hoaDon.getTrangThai()) && !"DA_HUY".equals(hoaDon.getTrangThai());

        // Nếu đơn online đang ở CHO_THANH_TOAN, kiểm tra xem có memo SePay đã được chuyển tiền (kể cả 1k)
        if (!paid && hoaDon.getMaHoaDon() != null) {
            String key = hoaDon.getMaHoaDon().trim().toUpperCase();
            activeListeningMemos.put(key, System.currentTimeMillis() + 10 * 60 * 1000L);
            BigDecimal received = sepayPendingMemoAmounts.get(key);
            if (received != null && received.compareTo(BigDecimal.ZERO) > 0) {
                try {
                    hoaDonService.xacNhanThanhToanVNPay(hoaDon.getId());
                    hoaDon = hoaDonRepository.findById(hoaDon.getId()).orElse(hoaDon);
                    paid = true;
                    sepayPendingMemoAmounts.remove(key);
                    activeListeningMemos.remove(key);
                    log.info("Polling kiemTraTrangThaiThanhToan: khớp pending memo SePay {} ({}đ) -> xác nhận thanh toán đơn #{}",
                        key, received, id);
                } catch (Exception e) {
                    log.error("Lỗi xác nhận thanh toán đơn #{} từ pending memo: {}", id, e.getMessage());
                }
            }
        }

        return Map.of(
            "paid", paid,
            "trangThai", hoaDon.getTrangThai() != null ? hoaDon.getTrangThai() : "",
            "orderId", hoaDon.getId(),
            "maHoaDon", hoaDon.getMaHoaDon() != null ? hoaDon.getMaHoaDon() : "",
            "amount", hoaDon.getTienSauGiamGia() != null ? hoaDon.getTienSauGiamGia() : BigDecimal.ZERO
        );
    }

    @Transactional
    public Map<String, Object> xuLyWebhookSepay(Map<String, Object> payload, String authHeader) {
        if (sepayWebhookApiKey != null && !sepayWebhookApiKey.isBlank()) {
            String expected = sepayWebhookApiKey.trim();
            String token = authHeader == null ? "" : authHeader.trim();
            if (token.regionMatches(true, 0, "Apikey ", 0, 7)) {
                token = token.substring(7).trim();
            } else if (token.regionMatches(true, 0, "Bearer ", 0, 7)) {
                token = token.substring(7).trim();
            }
            if (!expected.equals(token)) {
                throw new SecurityException("API key SePay không hợp lệ");
            }
        }

        if (payload == null) {
            return Map.of("success", false, "message", "Payload trống");
        }

        String transferType = String.valueOf(payload.getOrDefault("transferType", "in"));
        if (!"in".equalsIgnoreCase(transferType)) {
            return Map.of("success", true, "ignored", true, "message", "Bỏ qua giao dịch tiền ra");
        }

        String content = String.valueOf(payload.getOrDefault("content", payload.getOrDefault("description", "")));
        BigDecimal transferAmount = BigDecimal.ZERO;
        Object rawAmount = payload.get("transferAmount");
        if (rawAmount != null) {
            try {
                transferAmount = new BigDecimal(String.valueOf(rawAmount));
            } catch (NumberFormatException ignored) {}
        }

        Matcher matcher = MA_HOA_DON_PATTERN.matcher(content);
        HoaDon hoaDon = null;
        String matchedCode = null;
        while (matcher.find()) {
            String code = matcher.group(1).toUpperCase().replaceAll("\\s+", "");
            if (matchedCode == null) matchedCode = code;
            hoaDon = hoaDonRepository.findByMaHoaDon(code).orElse(null);
            if (hoaDon != null) break;
        }

        // Nếu chưa tìm thấy bằng regex, kiểm tra xem nội dung chuyển khoản có chứa memo đang được POS/FE lắng nghe không
        if (matchedCode == null) {
            String cleanContent = content.toUpperCase().replaceAll("\\s+", "");
            long now = System.currentTimeMillis();
            for (Map.Entry<String, Long> entry : activeListeningMemos.entrySet()) {
                if (entry.getValue() > now) {
                    String cleanKey = entry.getKey().replaceAll("\\s+", "");
                    if (cleanContent.contains(cleanKey)) {
                        matchedCode = entry.getKey();
                        hoaDon = hoaDonRepository.findByMaHoaDon(matchedCode).orElse(null);
                        break;
                    }
                }
            }
        }

        if (hoaDon == null) {
            if (matchedCode != null && transferAmount.compareTo(BigDecimal.ZERO) > 0) {
                final BigDecimal amt = transferAmount;
                sepayPendingMemoAmounts.merge(matchedCode, amt, BigDecimal::add);
                log.info("SePay webhook (POS/chờ): ghi nhận chuyển khoản {}đ cho mã {}", transferAmount, matchedCode);
                return Map.of("success", true, "matched", true, "pendingMemo", matchedCode, "amount", transferAmount);
            }
            log.info("SePay webhook: không tìm thấy mã hóa đơn trong nội dung '{}'", content);
            return Map.of("success", true, "matched", false, "message", "Không tìm thấy đơn hàng tương ứng");
        }

        // Theo yêu cầu: Chỉ cần chuyển tiền (kể cả 1k hoặc bất kỳ số tiền nào > 0) là tự động xác nhận thanh toán thành công và tắt QR
        if (transferAmount.compareTo(BigDecimal.ZERO) <= 0) {
            log.warn("SePay webhook: số tiền chuyển không hợp lệ ({}) cho đơn {}", transferAmount, hoaDon.getMaHoaDon());
            return Map.of("success", true, "matched", true, "paid", false, "message", "Số tiền chuyển không hợp lệ");
        }

        BigDecimal requiredAmount = hoaDon.getTienSauGiamGia() != null ? hoaDon.getTienSauGiamGia() : BigDecimal.ZERO;
        if ("CHO_THANH_TOAN".equals(hoaDon.getTrangThai())) {
            hoaDonService.xacNhanThanhToanVNPay(hoaDon.getId());
            hoaDon = hoaDonRepository.findById(hoaDon.getId()).orElse(hoaDon);
            String refCode = String.valueOf(payload.getOrDefault("referenceCode", payload.getOrDefault("id", "")));
            String sepayNote = String.format("[SePay chuyển khoản thành công | GD: %s | Đã nhận: %,.0fđ / Đơn: %,.0fđ]",
                refCode, transferAmount, requiredAmount);
            String currentNote = hoaDon.getGhiChu() != null ? hoaDon.getGhiChu().trim() : "";
            String updatedNote = currentNote.isEmpty() ? sepayNote : currentNote + " " + sepayNote;
            if (updatedNote.length() > 1000) updatedNote = updatedNote.substring(0, 1000);
            hoaDon.setGhiChu(updatedNote);
            hoaDon.setNgayCapNhat(LocalDateTime.now());
            hoaDonRepository.save(hoaDon);
            log.info("SePay webhook: đã xác nhận thanh toán đơn {} (nhận {}đ / giá trị {}đ)",
                hoaDon.getMaHoaDon(), transferAmount, requiredAmount);
        }

        // Đồng thời lưu vào pending memo để nếu FE đang polling theo memo cũng nhận được kết quả ngay
        sepayPendingMemoAmounts.put(hoaDon.getMaHoaDon().toUpperCase(), transferAmount);

        return Map.of("success", true, "matched", true, "paid", true, "maHoaDon", hoaDon.getMaHoaDon(), "receivedAmount", transferAmount);
    }

    public Map<String, Object> kiemTraMemoSepay(String memo, BigDecimal requiredAmount, boolean consume) {
        if (memo == null || memo.isBlank()) {
            return Map.of("paid", false);
        }
        String key = memo.trim().toUpperCase();
        activeListeningMemos.put(key, System.currentTimeMillis() + 10 * 60 * 1000L);

        BigDecimal received = sepayPendingMemoAmounts.get(key);
        // Chỉ cần có tiền chuyển vào (> 0, kể cả 1k hoặc bất kỳ số tiền nào) là chấp nhận thanh toán thành công
        boolean paid = received != null && received.compareTo(BigDecimal.ZERO) > 0;

        if (!paid) {
            HoaDon hd = hoaDonRepository.findByMaHoaDon(key).orElse(null);
            if (hd != null && !"CHO_THANH_TOAN".equals(hd.getTrangThai()) && !"DA_HUY".equals(hd.getTrangThai())) {
                paid = true;
                received = hd.getTienSauGiamGia();
            }
        }

        if (paid && consume) {
            sepayPendingMemoAmounts.remove(key);
            activeListeningMemos.remove(key);
        }
        return Map.of("paid", paid, "memo", key, "receivedAmount", received != null ? received : BigDecimal.ZERO);
    }
}
