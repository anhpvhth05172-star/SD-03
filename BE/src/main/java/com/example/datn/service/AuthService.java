package com.example.datn.service;

import com.example.datn.config.JwtService;
import com.example.datn.dto.AuthResponse;
import com.example.datn.dto.DangKyRequest;
import com.example.datn.dto.DangNhapRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.PhienDangNhap;
import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.PhienDangNhapRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private static final String EMAIL_REGEX = "^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$";

    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final PhienDangNhapRepository phienDangNhapRepository;

    private final long thoiGianPhienMs;

    public AuthService(
        KhachHangRepository khachHangRepository,
        PasswordEncoder passwordEncoder,
        JwtService jwtService,
        PhienDangNhapRepository phienDangNhapRepository,
        @Value("${app.auth.session-inactivity-ms:1800000}") long thoiGianPhienMs
    ) {
        this.khachHangRepository = khachHangRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.phienDangNhapRepository = phienDangNhapRepository;
        this.thoiGianPhienMs = thoiGianPhienMs;
    }

    @Transactional
    public KhachHang dangKy(DangKyRequest req) {
        if (req == null || isBlank(req.getTenTaiKhoan())) {
            throw new IllegalArgumentException("Tên tài khoản không được để trống");
        }
        String tenTaiKhoan = req.getTenTaiKhoan().trim();
        if (isBlank(req.getEmail())) {
            throw new IllegalArgumentException("Email không được để trống");
        }
        String email = req.getEmail().trim();
        if (!email.matches(EMAIL_REGEX)) {
            throw new IllegalArgumentException("Email không hợp lệ");
        }
        if (isBlank(req.getSoDienThoai())) {
            throw new IllegalArgumentException("Số điện thoại không được để trống");
        }
        if (isBlank(req.getMatKhau())) {
            throw new IllegalArgumentException("Mật khẩu không được để trống");
        }
        if (req.getMatKhau().length() < 6) {
            throw new IllegalArgumentException("Mật khẩu phải có ít nhất 6 ký tự");
        }
        if (!isBlank(req.getXacNhanMatKhau()) && !req.getMatKhau().equals(req.getXacNhanMatKhau())) {
            throw new IllegalArgumentException("Mật khẩu xác nhận không khớp");
        }
        if (khachHangRepository.existsByTenTaiKhoan(tenTaiKhoan)) {
            throw new IllegalArgumentException("Tên tài khoản đã tồn tại");
        }
        if (khachHangRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email đã được sử dụng");
        }

        KhachHang khachHang = new KhachHang();
        khachHang.setMaKhachHang(taoMaKhachHang());
        khachHang.setTenTaiKhoan(tenTaiKhoan);
        khachHang.setTenKhachHang(tenTaiKhoan);
        khachHang.setEmail(email);
        khachHang.setSoDienThoai(req.getSoDienThoai().trim());
        khachHang.setMatKhau(passwordEncoder.encode(req.getMatKhau()));
        khachHang.setTrangThai(true);
        khachHang.setVaiTro("USER");
        khachHang.setNguoiTao("he-thong");
        return khachHangRepository.save(khachHang);
    }

    @Transactional
    public AuthResponse dangNhap(DangNhapRequest req) {
        if (req == null || isBlank(req.getTaiKhoan()) || isBlank(req.getMatKhau())) {
            throw new DangNhapThatBaiException("Vui lòng nhập tài khoản và mật khẩu");
        }
        KhachHang khachHang = khachHangRepository
            .findByEmailOrTenTaiKhoan(req.getTaiKhoan().trim())
            .orElseThrow(() -> new DangNhapThatBaiException("Sai tài khoản hoặc mật khẩu"));
        if (!passwordEncoder.matches(req.getMatKhau(), khachHang.getMatKhau())) {
            throw new DangNhapThatBaiException("Sai tài khoản hoặc mật khẩu");
        }
        if (Boolean.FALSE.equals(khachHang.getTrangThai())) {
            throw new DangNhapThatBaiException("Tài khoản đã bị khóa");
        }

        LocalDateTime bayGio = LocalDateTime.now();
        String refreshToken = taoRefreshToken();
        PhienDangNhap phien = new PhienDangNhap();
        phien.setIdKhachHang(khachHang.getId());
        phien.setRefreshTokenHash(sha256(refreshToken));
        phien.setNgayTao(bayGio);
        phien.setHoatDongCuoi(bayGio);
        phien.setRevoked(false);
        phienDangNhapRepository.save(phien);

        return new AuthResponse(
            jwtService.taoToken(khachHang, phien.getId()),
            refreshToken,
            khachHang.getId(),
            khachHang.getTenTaiKhoan(),
            khachHang.getEmail(),
            khachHang.getTenKhachHang(),
            khachHang.getVaiTro()
        );
    }

    @Transactional
    public AuthResponse refresh(String refreshToken) {
        if (isBlank(refreshToken)) {
            throw new DangNhapThatBaiException("Phiên đăng nhập không hợp lệ");
        }
        PhienDangNhap phien = phienDangNhapRepository
            .findByRefreshTokenHash(sha256(refreshToken))
            .orElseThrow(() -> new DangNhapThatBaiException("Phiên đăng nhập không hợp lệ"));
        LocalDateTime bayGio = LocalDateTime.now();
        if (phien.isRevoked() || phien.hetHan(bayGio, thoiGianPhienMs)) {
            throw new DangNhapThatBaiException("Phiên đăng nhập đã hết hạn");
        }
        KhachHang khachHang = khachHangRepository
            .findById(phien.getIdKhachHang())
            .orElseThrow(() -> new DangNhapThatBaiException("Phiên đăng nhập không hợp lệ"));
        if (Boolean.FALSE.equals(khachHang.getTrangThai())) {
            throw new DangNhapThatBaiException("Tài khoản đã bị khóa");
        }

        phien.setHoatDongCuoi(bayGio);
        phienDangNhapRepository.save(phien);

        return new AuthResponse(
            jwtService.taoToken(khachHang, phien.getId()),
            refreshToken,
            khachHang.getId(),
            khachHang.getTenTaiKhoan(),
            khachHang.getEmail(),
            khachHang.getTenKhachHang(),
            khachHang.getVaiTro()
        );
    }

    @Transactional
    public void dangXuat(String refreshToken) {
        if (isBlank(refreshToken)) {
            return;
        }
        Optional<PhienDangNhap> phien = phienDangNhapRepository.findByRefreshTokenHash(sha256(refreshToken));
        if (phien.isPresent() && !phien.get().isRevoked()) {
            phien.get().setRevoked(true);
            phien.get().setNgayDangXuat(LocalDateTime.now());
            phienDangNhapRepository.save(phien.get());
        }
    }

    private static String taoRefreshToken() {
        byte[] bytes = new byte[48];
        ThreadLocalRandom.current().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private static String sha256(String giaTri) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(giaTri.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("Khong tim duoc SHA-256", e);
        }
    }

    private String taoMaKhachHang() {
        for (int i = 0; i < 100; i++) {
            String ma = "KH" + String.format("%07d", ThreadLocalRandom.current().nextInt(10_000_000));
            if (!khachHangRepository.existsByMaKhachHang(ma)) {
                return ma;
            }
        }
        return "KH" + (System.currentTimeMillis() % 100_000_000);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
