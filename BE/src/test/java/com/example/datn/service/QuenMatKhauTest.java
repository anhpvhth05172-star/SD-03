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

class QuenMatKhauTest {

    private static final String EMAIL = "nhanvien01@polyshoes.vn";

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

    private NhanVien nhanVienCoEmail() {
        NhanVien nv = new NhanVien();
        nv.setId(801L);
        nv.setTenTaiKhoan("nhanvien01");
        nv.setEmail(EMAIL);
        nv.setMatKhau("mat-khau-cu");
        VaiTro vaiTro = new VaiTro();
        vaiTro.setId(3L);
        vaiTro.setMaVaiTro("STAFF");
        nv.setVaiTro(vaiTro);
        return nv;
    }

    private void coNhanVienTrongDb() {
        when(nhanVienRepository.findByEmail(EMAIL)).thenReturn(Optional.of(nhanVienCoEmail()));
        when(nhanVienRepository.save(any(NhanVien.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void guiMaQuenMatKhau_ThanhCong_GuiMailVaTraVeMa() {
        coNhanVienTrongDb();
        when(emailService.guiMaQuenMatKhau(eq(EMAIL), anyString())).thenReturn(true);

        service.guiMaQuenMatKhau(EMAIL);

        ArgumentCaptor<String> maCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).guiMaQuenMatKhau(eq(EMAIL), maCaptor.capture());
        assertThat(maCaptor.getValue()).matches("\\d{6}");
    }

    @Test
    void guiMaQuenMatKhau_EmailKhongTonTai_BiTuChoi() {
        when(nhanVienRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.guiMaQuenMatKhau(EMAIL))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không tìm thấy tài khoản với email này");

        verify(emailService, never()).guiMaQuenMatKhau(anyString(), anyString());
    }

    @Test
    void guiMaQuenMatKhau_GuiMailThatBai_BaoLoi() {
        coNhanVienTrongDb();
        when(emailService.guiMaQuenMatKhau(eq(EMAIL), anyString())).thenReturn(false);

        assertThatThrownBy(() -> service.guiMaQuenMatKhau(EMAIL))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("Không gửi được mã xác nhận");
    }

    @Test
    void datLaiMatKhau_ThanhCong_VoiMaDung() {
        coNhanVienTrongDb();
        String ma = otpService.sinhMa(EMAIL);

        service.datLaiMatKhau(EMAIL, ma, "matKhauMoi123", "matKhauMoi123");

        ArgumentCaptor<NhanVien> captor = ArgumentCaptor.forClass(NhanVien.class);
        verify(nhanVienRepository).save(captor.capture());
        assertThat(passwordEncoder.matches("matKhauMoi123", captor.getValue().getMatKhau())).isTrue();
    }

    @Test
    void datLaiMatKhau_MaSai_BiTuChoi() {
        coNhanVienTrongDb();
        otpService.sinhMa(EMAIL);

        assertThatThrownBy(() -> service.datLaiMatKhau(EMAIL, "000000", "matKhauMoi123", "matKhauMoi123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Mã xác nhận không đúng");

        verify(nhanVienRepository, never()).save(any(NhanVien.class));
    }

    @Test
    void datLaiMatKhau_SaiQuaNamLan_MaTuHuy() {
        coNhanVienTrongDb();
        String ma = otpService.sinhMa(EMAIL);
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> service.datLaiMatKhau(EMAIL, "999999", "matKhauMoi123", "matKhauMoi123"))
                .isInstanceOf(IllegalArgumentException.class);
        }

        assertThatThrownBy(() -> service.datLaiMatKhau(EMAIL, ma, "matKhauMoi123", "matKhauMoi123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hết hiệu lực");

        verify(nhanVienRepository, never()).save(any(NhanVien.class));
    }

    @Test
    void datLaiMatKhau_MatKhauMoiQuaNgan_BiTuChoi() {
        String ma = otpService.sinhMa(EMAIL);

        assertThatThrownBy(() -> service.datLaiMatKhau(EMAIL, ma, "12345", "12345"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("6 đến 127");
    }

    @Test
    void datLaiMatKhau_XacNhanKhongKhop_BiTuChoi() {
        String ma = otpService.sinhMa(EMAIL);

        assertThatThrownBy(() -> service.datLaiMatKhau(EMAIL, ma, "matKhauMoi123", "khacNhau123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("xác nhận không khớp");
    }

    @Test
    void otpHetHan_SauThoiGianChoPhep() throws InterruptedException {
        QuenMatKhauOtpService otpNgan = new QuenMatKhauOtpService(50L, 0L, 5);
        String ma = otpNgan.sinhMa(EMAIL);
        Thread.sleep(80);

        assertThatThrownBy(() -> otpNgan.xacNhan(EMAIL, ma))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("hết hiệu lực");
    }
}
