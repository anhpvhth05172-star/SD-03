package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.config.JwtService;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhienDangNhapRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class QuenMatKhauKhachHangTest {

    private static final String EMAIL_KH = "khach01@gmail.com";
    private static final String EMAIL_NV = "nhanvien01@polyshoes.vn";
    private static final String KHOA_OTP_KHACH = "KH:";

    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final EmailService emailService = mock(EmailService.class);
    private final QuenMatKhauOtpService otpService = new QuenMatKhauOtpService(600_000L, 60_000L, 5);

    private final AuthService service = new AuthService(
        khachHangRepository,
        nhanVienRepository,
        passwordEncoder,
        mock(JwtService.class),
        mock(PhienDangNhapRepository.class),
        1800000L,
        emailService,
        otpService,
        2592000000L
    );

    private KhachHang khachHangCoEmail() {
        KhachHang kh = new KhachHang();
        kh.setId(901L);
        kh.setTenTaiKhoan("khach01");
        kh.setTenKhachHang("Khách Một");
        kh.setEmail(EMAIL_KH);
        kh.setMatKhau("mat-khau-cu");
        return kh;
    }

    private NhanVien nhanVienCoEmail() {
        NhanVien nv = new NhanVien();
        nv.setId(801L);
        nv.setTenTaiKhoan("nhanvien01");
        nv.setEmail(EMAIL_NV);
        nv.setMatKhau("mat-khau-cu");
        VaiTro vaiTro = new VaiTro();
        vaiTro.setId(3L);
        vaiTro.setMaVaiTro("STAFF");
        nv.setVaiTro(vaiTro);
        return nv;
    }

    @Test
    void guiMaQuenMatKhauChung_EmailKhachHang_GuiMaOtpChoKhach() {
        when(nhanVienRepository.findByEmail(EMAIL_KH)).thenReturn(Optional.empty());
        when(khachHangRepository.findByEmail(EMAIL_KH)).thenReturn(Optional.of(khachHangCoEmail()));
        when(emailService.guiMaQuenMatKhau(eq(EMAIL_KH), anyString())).thenReturn(true);

        service.guiMaQuenMatKhauChung(EMAIL_KH);

        ArgumentCaptor<String> maCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).guiMaQuenMatKhau(eq(EMAIL_KH), maCaptor.capture());
        assertThat(maCaptor.getValue()).matches("\\d{6}");
    }

    @Test
    void guiMaQuenMatKhauChung_EmailNhanVien_UuTienLuongNhanVien() {
        when(nhanVienRepository.findByEmail(EMAIL_NV)).thenReturn(Optional.of(nhanVienCoEmail()));
        when(emailService.guiMaQuenMatKhau(eq(EMAIL_NV), anyString())).thenReturn(true);

        service.guiMaQuenMatKhauChung(EMAIL_NV);

        ArgumentCaptor<String> maCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).guiMaQuenMatKhau(eq(EMAIL_NV), maCaptor.capture());
        assertThat(maCaptor.getValue()).matches("\\d{6}");
        verify(khachHangRepository, never()).findByEmail(anyString());
    }

    @Test
    void guiMaQuenMatKhauChung_EmailKhongTonTai_BiTuChoi() {
        when(nhanVienRepository.findByEmail(EMAIL_KH)).thenReturn(Optional.empty());
        when(khachHangRepository.findByEmail(EMAIL_KH)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.guiMaQuenMatKhauChung(EMAIL_KH))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không tìm thấy tài khoản với email này");

        verify(emailService, never()).guiMaQuenMatKhau(anyString(), anyString());
    }

    @Test
    void datLaiMatKhauChung_KhachHang_TraMatKhauMoi() {
        when(nhanVienRepository.findByEmail(EMAIL_KH)).thenReturn(Optional.empty());
        when(khachHangRepository.findByEmail(EMAIL_KH)).thenReturn(Optional.of(khachHangCoEmail()));
        String ma = otpService.sinhMa(KHOA_OTP_KHACH + EMAIL_KH);

        service.datLaiMatKhauChung(EMAIL_KH, ma, "matKhauMoi123", "matKhauMoi123");

        ArgumentCaptor<KhachHang> captor = ArgumentCaptor.forClass(KhachHang.class);
        verify(khachHangRepository).save(captor.capture());
        assertThat(passwordEncoder.matches("matKhauMoi123", captor.getValue().getMatKhau())).isTrue();
    }

    @Test
    void datLaiMatKhauChung_KhachHang_MaSai_BiTuChoi() {
        when(nhanVienRepository.findByEmail(EMAIL_KH)).thenReturn(Optional.empty());
        when(khachHangRepository.findByEmail(EMAIL_KH)).thenReturn(Optional.of(khachHangCoEmail()));
        otpService.sinhMa(KHOA_OTP_KHACH + EMAIL_KH);

        assertThatThrownBy(() -> service.datLaiMatKhauChung(EMAIL_KH, "000000", "matKhauMoi123", "matKhauMoi123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mã xác nhận không đúng");

        verify(khachHangRepository, never()).save(any(KhachHang.class));
    }
}
