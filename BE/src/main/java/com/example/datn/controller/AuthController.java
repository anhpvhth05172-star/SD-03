package com.example.datn.controller;

import com.example.datn.dto.Auth.AuthResponse;
import com.example.datn.dto.Auth.DangKyRequest;
import com.example.datn.dto.Auth.DangNhapRequest;
import com.example.datn.dto.Auth.DangXuatRequest;
import com.example.datn.dto.Auth.RefreshRequest;
import com.example.datn.dto.Auth.ThongTinResponse;
import com.example.datn.entity.KhachHang;
import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.service.AuthService;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final KhachHangRepository khachHangRepository;
    private final NhanVienRepository nhanVienRepository;

    @PostMapping("/dang-ky")
    public ResponseEntity<Map<String, Object>> dangKy(@RequestBody DangKyRequest req) {
        KhachHang khachHang = authService.dangKy(req);
        Map<String, Object> body = new HashMap<>();
        body.put("message", "Đăng ký thành công");
        body.put("id", khachHang.getId());
        body.put("tenTaiKhoan", khachHang.getTenTaiKhoan());
        body.put("email", khachHang.getEmail());
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    @PostMapping("/dang-nhap")
    public AuthResponse dangNhap(@RequestBody DangNhapRequest req) {
        return authService.dangNhap(req);
    }

    @PostMapping("/nhan-vien/dang-nhap")
    public AuthResponse dangNhapNhanVien(@RequestBody DangNhapRequest req) {
        return authService.dangNhapNhanVien(req);
    }

    @PostMapping("/refresh")
    public AuthResponse refresh(@RequestBody RefreshRequest req) {
        return authService.refresh(req == null ? null : req.refreshToken());
    }

    @PostMapping("/dang-xuat")
    public ResponseEntity<Map<String, String>> dangXuat(@RequestBody(required = false) DangXuatRequest req) {
        authService.dangXuat(req == null ? null : req.refreshToken());
        Map<String, String> body = new HashMap<>();
        body.put("message", "Đăng xuất thành công");
        return ResponseEntity.ok(body);
    }

    @GetMapping("/thong-tin")
    public ThongTinResponse thongTin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof Long idNguoiDung)) {
            throw new DangNhapThatBaiException("Chưa đăng nhập");
        }
        boolean laNhanVien = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_NHAN_VIEN".equals(a.getAuthority()));
        if (laNhanVien) {
            var nhanVien = nhanVienRepository.findById(idNguoiDung)
                .orElseThrow(() -> new DangNhapThatBaiException("Không tìm thấy tài khoản"));
            String vaiTro = nhanVien.getVaiTro() != null ? nhanVien.getVaiTro().getMaVaiTro() : "STAFF";
            return new ThongTinResponse(
                nhanVien.getId(),
                nhanVien.getTenTaiKhoan(),
                nhanVien.getTenTaiKhoan(),
                nhanVien.getEmail(),
                nhanVien.getSoDienThoai(),
                vaiTro
            );
        }
        KhachHang khachHang = khachHangRepository.findById(idNguoiDung)
            .orElseThrow(() -> new DangNhapThatBaiException("Không tìm thấy tài khoản"));
        return new ThongTinResponse(
            khachHang.getId(),
            khachHang.getTenTaiKhoan(),
            khachHang.getTenKhachHang(),
            khachHang.getEmail(),
            khachHang.getSoDienThoai(),
            khachHang.getVaiTro()
        );
    }
}
