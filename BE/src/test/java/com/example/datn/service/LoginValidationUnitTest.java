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
import com.example.datn.dto.Auth.AuthResponse;
import com.example.datn.dto.Auth.DangNhapRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhienDangNhap;
import com.example.datn.entity.VaiTro;
import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhienDangNhapRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class LoginValidationUnitTest {

    private static final String JWT_SECRET = "VGVzdC1KV1QtU2VjcmV0LUtleS0wMTIzNDU2Nzg5QUJDREVGR0g=";

    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);
    private final PhienDangNhapRepository phienDangNhapRepository = mock(PhienDangNhapRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(JWT_SECRET, 3_600_000L, 3_600_000L);

    private final AuthService authService = new AuthService(
        khachHangRepository,
        nhanVienRepository,
        passwordEncoder,
        jwtService,
        phienDangNhapRepository,
        1_800_000L,
        mock(EmailService.class),
        new QuenMatKhauOtpService(),
        2_592_000_000L
    );

    private DangNhapRequest req(String tk, String mk) {
        return new DangNhapRequest(tk, mk, false);
    }

    @Test
    void khachHangVaNhanVien_caHaiTrong_baoLoiChungVaKhongQueryDb() {
        assertThatThrownBy(() -> authService.dangNhap(req("", "")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Vui lòng nhập tài khoản và mật khẩu");
        assertThatThrownBy(() -> authService.dangNhapNhanVien(req("   ", "")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Vui lòng nhập tài khoản và mật khẩu");
        verify(khachHangRepository, never()).findByEmailOrTenTaiKhoan(anyString());
        verify(nhanVienRepository, never()).findByEmailOrTenTaiKhoan(anyString());
    }

    @Test
    void taiKhoanTrongHoacKhoangTrang_baoLoiTrongTaiKhoan() {
        assertThatThrownBy(() -> authService.dangNhap(req("", "123456")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Tài khoản / Email không được để trống");
        assertThatThrownBy(() -> authService.dangNhapNhanVien(req("   ", "123456")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Tài khoản / Email không được để trống");
    }

    @Test
    void taiKhoanQua150KyTu_baoLoiVuotDoDai() {
        String over = "a".repeat(151);
        assertThatThrownBy(() -> authService.dangNhap(req(over, "123456")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Tài khoản / Email không được vượt quá 150 ký tự");
        assertThatThrownBy(() -> authService.dangNhapNhanVien(req(over, "123456")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Tài khoản / Email không được vượt quá 150 ký tự");
    }

    @Test
    void taiKhoanChuaHtmlXssHoacDieuKhien_biTuChoi() {
        assertThatThrownBy(() -> authService.dangNhap(req("<script>alert(1)</script>", "123456")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Tài khoản / Email chứa ký tự không hợp lệ");
        assertThatThrownBy(() -> authService.dangNhapNhanVien(req("admin\nDROP", "123456")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Tài khoản / Email chứa ký tự không hợp lệ");
    }

    @Test
    void emailSaiDinhDang_biTuChoiTruocKhiQueryDb() {
        for (String badEmail : new String[] { "a@", "@b.com", "a@b", "a b@c.com" }) {
            assertThatThrownBy(() -> authService.dangNhap(req(badEmail, "123456")))
                .isInstanceOf(DangNhapThatBaiException.class)
                .hasMessage("Email không hợp lệ");
            assertThatThrownBy(() -> authService.dangNhapNhanVien(req(badEmail, "123456")))
                .isInstanceOf(DangNhapThatBaiException.class)
                .hasMessage("Email không hợp lệ");
        }
        verify(khachHangRepository, never()).findByEmailOrTenTaiKhoan(anyString());
        verify(nhanVienRepository, never()).findByEmailOrTenTaiKhoan(anyString());
    }

    @Test
    void matKhauTrongKhoangTrangNganHoacDaiQua_biTuChoi() {
        assertThatThrownBy(() -> authService.dangNhap(req("smokekh01", "")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Mật khẩu không được để trống");
        assertThatThrownBy(() -> authService.dangNhapNhanVien(req("admin", "      ")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Mật khẩu không được chỉ chứa khoảng trắng");
        assertThatThrownBy(() -> authService.dangNhap(req("smokekh01", "12345")))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Mật khẩu phải có ít nhất 6 ký tự");
        assertThatThrownBy(() -> authService.dangNhapNhanVien(req("admin", "a".repeat(128))))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Mật khẩu không được vượt quá 127 ký tự");
    }

    @Test
    void bienHopLe_6Va127KyTu_trimVaTenTiengVietCoDau_dangNhapDuoc() {
        String mk6 = "123456";
        String mk127 = "x".repeat(127);

        KhachHang kh = new KhachHang();
        kh.setId(10L);
        kh.setTenTaiKhoan("smokekh01");
        kh.setEmail("smoke01@example.com");
        kh.setTenKhachHang("Khách Smoke");
        kh.setVaiTro("USER");
        kh.setTrangThai(true);
        kh.setMatKhau(passwordEncoder.encode(mk6));
        when(khachHangRepository.findByEmailOrTenTaiKhoan("smoke01@example.com")).thenReturn(Optional.of(kh));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> {
            PhienDangNhap p = inv.getArgument(0);
            p.setId(1L);
            return p;
        });

        AuthResponse khResp = authService.dangNhap(req("  smoke01@example.com  ", mk6));
        assertThat(khResp.getToken()).isNotBlank();
        assertThat(khResp.getTenTaiKhoan()).isEqualTo("smokekh01");

        NhanVien nv = new NhanVien();
        nv.setId(4L);
        nv.setTenTaiKhoan("Lê Huỳnh Đức");
        nv.setEmail("duc@gmail.com");
        nv.setTrangThai(true);
        nv.setMatKhau(mk127);
        VaiTro vt = new VaiTro();
        vt.setMaVaiTro("STAFF");
        nv.setVaiTro(vt);
        when(nhanVienRepository.findByEmailOrTenTaiKhoan("Lê Huỳnh Đức")).thenReturn(Optional.of(nv));

        AuthResponse nvResp = authService.dangNhapNhanVien(req("  Lê Huỳnh Đức  ", mk127));
        assertThat(nvResp.getToken()).isNotBlank();
        assertThat(nvResp.getVaiTro()).isEqualTo("STAFF");

        // BCrypt gioi han 72 byte: nhap 127 ky tu voi tai khoan BCrypt khong duoc crash IllegalArgumentException
        NhanVien nvBcrypt = new NhanVien();
        nvBcrypt.setId(5L);
        nvBcrypt.setTenTaiKhoan("admin");
        nvBcrypt.setTrangThai(true);
        nvBcrypt.setMatKhau(passwordEncoder.encode("123123"));
        nvBcrypt.setVaiTro(vt);
        when(nhanVienRepository.findByEmailOrTenTaiKhoan("admin")).thenReturn(Optional.of(nvBcrypt));

        assertThatThrownBy(() -> authService.dangNhapNhanVien(req("admin", mk127)))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Sai tài khoản hoặc mật khẩu");
    }
}
