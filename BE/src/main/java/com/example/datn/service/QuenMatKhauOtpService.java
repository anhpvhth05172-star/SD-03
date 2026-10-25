package com.example.datn.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.stereotype.Service;

/**
 * Lưu trữ OTP đặt lại mật khẩu tạm thời trong bộ nhớ (không đổi schema).
 * OTP có hạn 10 phút, gửi lại sau 60 giây, sai 5 lần tự hủy.
 */
@Service
public class QuenMatKhauOtpService {

    private static final long TTL_MS_MAC_DINH = 10 * 60_000L;
    private static final long COOLDOWN_MS_MAC_DINH = 60_000L;
    private static final int TOI_DA_SAI_MAC_DINH = 5;

    private final Map<String, BanGhiOtp> bang = new ConcurrentHashMap<>();
    private final long ttlMs;
    private final long cooldownMs;
    private final int toiDaSai;

    public QuenMatKhauOtpService() {
        this(TTL_MS_MAC_DINH, COOLDOWN_MS_MAC_DINH, TOI_DA_SAI_MAC_DINH);
    }

    QuenMatKhauOtpService(long ttlMs, long cooldownMs, int toiDaSai) {
        this.ttlMs = ttlMs;
        this.cooldownMs = cooldownMs;
        this.toiDaSai = toiDaSai;
    }

    public synchronized String sinhMa(String email) {
        String khoa = chuanHoaKhoa(email);
        long now = System.currentTimeMillis();
        BanGhiOtp cu = bang.get(khoa);
        if (cu != null && now - cu.lanGuiCuoi < cooldownMs) {
            throw new IllegalArgumentException("Vui lòng chờ ít nhất 60 giây rồi gửi lại mã");
        }
        String ma = String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
        bang.put(khoa, new BanGhiOtp(ma, now + ttlMs, now));
        return ma;
    }

    public synchronized void xacNhan(String email, String ma) {
        String khoa = chuanHoaKhoa(email);
        BanGhiOtp banGhi = bang.get(khoa);
        if (banGhi == null || System.currentTimeMillis() > banGhi.hanHetHetHan || banGhi.soLanSai >= toiDaSai) {
            bang.remove(khoa);
            throw new IllegalArgumentException("Mã xác nhận không đúng hoặc đã hết hiệu lực, vui lòng gửi lại mã");
        }
        if (!banGhi.ma.equals(ma)) {
            banGhi.soLanSai++;
            if (banGhi.soLanSai >= toiDaSai) {
                bang.remove(khoa);
                throw new IllegalArgumentException("Mã xác nhận không đúng hoặc đã hết hiệu lực, vui lòng gửi lại mã");
            }
            throw new IllegalArgumentException("Mã xác nhận không đúng");
        }
        bang.remove(khoa);
    }

    private static String chuanHoaKhoa(String email) {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        return email.trim().toLowerCase();
    }

    private static final class BanGhiOtp {
        private final String ma;
        private final long hanHetHetHan;
        private final long lanGuiCuoi;
        private int soLanSai;

        private BanGhiOtp(String ma, long hanHetHetHan, long lanGuiCuoi) {
            this.ma = ma;
            this.hanHetHetHan = hanHetHetHan;
            this.lanGuiCuoi = lanGuiCuoi;
        }
    }
}
