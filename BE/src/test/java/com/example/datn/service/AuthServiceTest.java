package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.config.JwtService;
import com.example.datn.dto.DangKyRequest;
import com.example.datn.dto.DangNhapRequest;
import com.example.datn.dto.AuthResponse;
import com.example.datn.entity.KhachHang;
import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.repository.KhachHangRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class AuthServiceTest {

    private static final String JWT_SECRET = "VGVzdC1KV1QtU2VjcmV0LUtleS0wMTIzNDU2Nzg5QUJDREVGR0g=";

    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(JWT_SECRET, 3_600_000L);
    private final AuthService service = new AuthService(khachHangRepository, passwordEncoder, jwtService);

    private DangKyRequest dangKyHopLe() {
        DangKyRequest req = new DangKyRequest();
        req.setTenTaiKhoan("nguyenvana");
        req.setEmail("nguyenvana@example.com");
        req.setSoDienThoai("0912345678");
        req.setMatKhau("matkhau123");
        req.setXacNhanMatKhau("matkhau123");
        return req;
    }

    @Test
    void test3_dangKyHopLe_PasswordDuocHashBangBCrypt() {
        when(khachHangRepository.existsByTenTaiKhoan("nguyenvana")).thenReturn(false);
        when(khachHangRepository.existsByEmail("nguyenvana@example.com")).thenReturn(false);
        when(khachHangRepository.existsByMaKhachHang(anyString())).thenReturn(false);
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> {
            KhachHang kh = inv.getArgument(0);
            kh.setId(1L);
            return kh;
        });

        KhachHang kh = service.dangKy(dangKyHopLe());

        assertThat(kh.getId()).isEqualTo(1L);
        assertThat(kh.getMaKhachHang()).startsWith("KH");
        assertThat(kh.getMatKhau()).isNotEqualTo("matkhau123");
        assertThat(kh.getMatKhau()).startsWith("$2");
        assertThat(passwordEncoder.matches("matkhau123", kh.getMatKhau())).isTrue();
        assertThat(kh.getTrangThai()).isTrue();
        assertThat(kh.getEmail()).isEqualTo("nguyenvana@example.com");
        assertThat(kh.getSoDienThoai()).isEqualTo("0912345678");
    }

    @Test
    void test3_trungTenTaiKhoan_ThatBai() {
        when(khachHangRepository.existsByTenTaiKhoan("nguyenvana")).thenReturn(true);

        assertThatThrownBy(() -> service.dangKy(dangKyHopLe()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên tài khoản đã tồn tại");
        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }

    @Test
    void test3_trungEmail_ThatBai() {
        when(khachHangRepository.existsByTenTaiKhoan("nguyenvana")).thenReturn(false);
        when(khachHangRepository.existsByEmail("nguyenvana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.dangKy(dangKyHopLe()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email đã được sử dụng");
        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }

    @Test
    void test3_matKhauQuaNgan_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setMatKhau("12345");
        req.setXacNhanMatKhau("12345");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ít nhất 6 ký tự");
    }

    @Test
    void test3_xacNhanMatKhauKhongKhop_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setXacNhanMatKhau("khacnhau");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không khớp");
    }

    @Test
    void test4_dangNhapDung_TokenHopLe() {
        String matKhau = "matkhau123";
        KhachHang kh = new KhachHang();
        kh.setId(5L);
        kh.setTenTaiKhoan("nguyenvana");
        kh.setEmail("nguyenvana@example.com");
        kh.setTenKhachHang("nguyenvana");
        kh.setMatKhau(passwordEncoder.encode(matKhau));
        kh.setTrangThai(true);
        when(khachHangRepository.findByEmailOrTenTaiKhoan("nguyenvana"))
            .thenReturn(Optional.of(kh));

        DangNhapRequest req = new DangNhapRequest("nguyenvana", matKhau);
        AuthResponse resp = service.dangNhap(req);

        assertThat(resp.getToken()).isNotBlank();
        assertThat(jwtService.hopLe(resp.getToken())).isTrue();
        assertThat(jwtService.layIdKhachHang(resp.getToken())).isEqualTo(5L);
        assertThat(resp.getIdKhachHang()).isEqualTo(5L);
        assertThat(resp.getTenTaiKhoan()).isEqualTo("nguyenvana");
        assertThat(resp.getEmail()).isEqualTo("nguyenvana@example.com");
    }

    @Test
    void test4_dangNhapBangEmail_CungPass_Thang() {
        String matKhau = "matkhau123";
        KhachHang kh = new KhachHang();
        kh.setId(6L);
        kh.setTenTaiKhoan("nguyenvana");
        kh.setEmail("nguyenvana@example.com");
        kh.setMatKhau(passwordEncoder.encode(matKhau));
        kh.setTrangThai(true);
        when(khachHangRepository.findByEmailOrTenTaiKhoan("nguyenvana@example.com"))
            .thenReturn(Optional.of(kh));

        AuthResponse resp = service.dangNhap(new DangNhapRequest("nguyenvana@example.com", matKhau));
        assertThat(jwtService.layIdKhachHang(resp.getToken())).isEqualTo(6L);
    }

    @Test
    void test4_dangNhapSaiMatKhau_ThatBai() {
        KhachHang kh = new KhachHang();
        kh.setId(5L);
        kh.setTenTaiKhoan("nguyenvana");
        kh.setMatKhau(passwordEncoder.encode("matkhau123"));
        kh.setTrangThai(true);
        when(khachHangRepository.findByEmailOrTenTaiKhoan("nguyenvana"))
            .thenReturn(Optional.of(kh));

        assertThatThrownBy(() -> service.dangNhap(new DangNhapRequest("nguyenvana", "saimatkhau")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessageContaining("Sai tài khoản hoặc mật khẩu");
    }

    @Test
    void test4_dangNhapTaiKhoanKhongTonTai_ThatBai() {
        when(khachHangRepository.findByEmailOrTenTaiKhoan("khongco"))
            .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.dangNhap(new DangNhapRequest("khongco", "batki")))
            .isInstanceOf(DangNhapThatBaiException.class);
    }

    @Test
    void dangKyKhongLuuPlainPassword() {
        when(khachHangRepository.existsByTenTaiKhoan(anyString())).thenReturn(false);
        when(khachHangRepository.existsByEmail(anyString())).thenReturn(false);
        when(khachHangRepository.existsByMaKhachHang(anyString())).thenReturn(false);
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));

        service.dangKy(dangKyHopLe());

        ArgumentCaptor<KhachHang> cap = ArgumentCaptor.forClass(KhachHang.class);
        verify(khachHangRepository).save(cap.capture());
        assertThat(cap.getValue().getMatKhau()).isNotEqualTo("matkhau123");
        assertThat(new BCryptPasswordEncoder().matches("matkhau123", cap.getValue().getMatKhau())).isTrue();
    }
}
