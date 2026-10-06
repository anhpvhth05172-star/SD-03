package com.example.datn.service;

import com.example.datn.config.JwtService;
import com.example.datn.dto.AuthResponse;
import com.example.datn.dto.DangKyRequest;
import com.example.datn.dto.DangNhapRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.repository.KhachHangRepository;
import java.util.concurrent.ThreadLocalRandom;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String EMAIL_REGEX = "^[\\w.%+-]+@[\\w.-]+\\.[A-Za-z]{2,}$";

    private final KhachHangRepository khachHangRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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

    @Transactional(readOnly = true)
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
        return new AuthResponse(
            jwtService.taoToken(khachHang),
            khachHang.getId(),
            khachHang.getTenTaiKhoan(),
            khachHang.getEmail(),
            khachHang.getTenKhachHang(),
            khachHang.getVaiTro()
        );
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
