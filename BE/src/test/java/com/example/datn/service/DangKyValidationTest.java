package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.config.JwtService;
import com.example.datn.dto.Auth.DangKyRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhienDangNhapRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class DangKyValidationTest {

    private static final String JWT_SECRET = "VGVzdC1KV1QtU2VjcmV0LUtleS0wMTIzNDU2Nzg5QUJDREVGR0g=";

    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);
    private final PhienDangNhapRepository phienDangNhapRepository = mock(PhienDangNhapRepository.class);
    private final EmailService emailService = mock(EmailService.class);
    private final QuenMatKhauOtpService quenMatKhauOtpService = mock(QuenMatKhauOtpService.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(JWT_SECRET, 3_600_000L, 3_600_000L);
    private final AuthService service = new AuthService(
        khachHangRepository,
        nhanVienRepository,
        passwordEncoder,
        jwtService,
        phienDangNhapRepository,
        1_800_000L,
        emailService,
        quenMatKhauOtpService,
        2_592_000_000L
    );

    private DangKyRequest dangKyHopLe() {
        DangKyRequest req = new DangKyRequest();
        req.setTenTaiKhoan("nguyenvana");
        req.setEmail("nguyenvana@example.com");
        req.setSoDienThoai("0912345678");
        req.setMatKhau("matkhau123");
        req.setXacNhanMatKhau("matkhau123");
        return req;
    }

    private void mockKhongTrung() {
        when(khachHangRepository.existsByTenTaiKhoan(anyString())).thenReturn(false);
        when(khachHangRepository.existsByEmail(anyString())).thenReturn(false);
        when(khachHangRepository.existsByMaKhachHang(anyString())).thenReturn(false);
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> {
            KhachHang kh = inv.getArgument(0);
            kh.setId(1L);
            return kh;
        });
    }

    @Test
    void dangKyHopLe_TaoDuocKhachHang() {
        mockKhongTrung();

        KhachHang kh = service.dangKy(dangKyHopLe());

        assertThat(kh.getId()).isEqualTo(1L);
        assertThat(kh.getMaKhachHang()).startsWith("KH");
        assertThat(kh.getMatKhau()).startsWith("$2");
        assertThat(passwordEncoder.matches("matkhau123", kh.getMatKhau())).isTrue();
        assertThat(kh.getSoDienThoai()).isEqualTo("0912345678");
    }

    @Test
    void soDienThoai9So_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setSoDienThoai("091234567");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số điện thoại phải bắt đầu bằng số 0 và gồm đúng 10 chữ số");
        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }

    @Test
    void soDienThoai11So_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setSoDienThoai("09123456789");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số điện thoại");
    }

    @Test
    void soDienThoaiKhongBatDauBang0_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setSoDienThoai("9123456789");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Số điện thoại");
    }

    @Test
    void emailSaiDinhDang_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setEmail("khongphai-email");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email không hợp lệ");
    }

    @Test
    void emailQuaDai_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setEmail("a".repeat(141) + "@example.com");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email không được vượt quá 150 ký tự");
    }

    @Test
    void tenTaiKhoanQuaDai_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setTenTaiKhoan("a".repeat(101));

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên tài khoản không được vượt quá 100 ký tự");
    }

    @Test
    void trungTenTaiKhoan_ThatBai() {
        when(khachHangRepository.existsByTenTaiKhoan("nguyenvana")).thenReturn(true);

        assertThatThrownBy(() -> service.dangKy(dangKyHopLe()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Tên tài khoản đã tồn tại");
        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }

    @Test
    void trungEmail_ThatBai() {
        when(khachHangRepository.existsByTenTaiKhoan("nguyenvana")).thenReturn(false);
        when(khachHangRepository.existsByEmail("nguyenvana@example.com")).thenReturn(true);

        assertThatThrownBy(() -> service.dangKy(dangKyHopLe()))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Email đã được sử dụng");
        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }

    @Test
    void matKhauQuaNgan_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setMatKhau("12345");
        req.setXacNhanMatKhau("12345");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mật khẩu phải có từ 6 đến 127 ký tự");
    }

    @Test
    void xacNhanMatKhauKhongKhop_ThatBai() {
        DangKyRequest req = dangKyHopLe();
        req.setXacNhanMatKhau("khacnhau");

        assertThatThrownBy(() -> service.dangKy(req))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("không khớp");
    }
}
