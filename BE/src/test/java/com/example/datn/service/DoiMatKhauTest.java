package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

class DoiMatKhauTest {

    private static final Long NV_ID = 800L;
    private static final Long KH_ID = 900L;

    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private final AuthService service = new AuthService(
        khachHangRepository,
        nhanVienRepository,
        passwordEncoder,
        mock(com.example.datn.config.JwtService.class),
        mock(PhienDangNhapRepository.class),
        1800000L,
        mock(EmailService.class),
        new QuenMatKhauOtpService(),
        2592000000L
    );

    private NhanVien nhanVien(String maVaiTro) {
        NhanVien nv = new NhanVien();
        nv.setId(NV_ID);
        nv.setTenTaiKhoan("nhanvien01");
        nv.setMatKhau("mat-khau-cu-thuan");
        if (maVaiTro != null) {
            VaiTro vaiTro = new VaiTro();
            vaiTro.setId(1L);
            vaiTro.setMaVaiTro(maVaiTro);
            nv.setVaiTro(vaiTro);
        }
        return nv;
    }

    private KhachHang khachHang() {
        KhachHang kh = new KhachHang();
        kh.setId(KH_ID);
        kh.setTenTaiKhoan("khach01");
        kh.setMatKhau(passwordEncoder.encode("123456"));
        return kh;
    }

    @Test
    void doiMatKhauNhanVien_ThanhCong_LuuBcrypt() {
        NhanVien nv = nhanVien("STAFF");
        when(nhanVienRepository.findById(NV_ID)).thenReturn(Optional.of(nv));
        when(nhanVienRepository.save(any(NhanVien.class))).thenAnswer(inv -> inv.getArgument(0));

        service.doiMatKhauNhanVien(NV_ID, "matKhauMoi123", "matKhauMoi123");

        ArgumentCaptor<NhanVien> captor = ArgumentCaptor.forClass(NhanVien.class);
        verify(nhanVienRepository).save(captor.capture());
        String daLuu = captor.getValue().getMatKhau();
        assertThat(daLuu).isNotEqualTo("matKhauMoi123").startsWith("$2");
        assertThat(passwordEncoder.matches("matKhauMoi123", daLuu)).isTrue();
    }

    @Test
    void doiMatKhauNhanVien_TaiKhoanAdmin_BiTuChoi() {
        NhanVien nv = nhanVien("ADMIN");
        when(nhanVienRepository.findById(NV_ID)).thenReturn(Optional.of(nv));

        assertThatThrownBy(() -> service.doiMatKhauNhanVien(NV_ID, "matKhauMoi123", "matKhauMoi123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không được đổi mật khẩu tài khoản quản trị");

        verify(nhanVienRepository, never()).save(any(NhanVien.class));
    }

    @Test
    void doiMatKhauNhanVien_MatKhauQuaNgan_BiTuChoi() {
        assertThatThrownBy(() -> service.doiMatKhauNhanVien(NV_ID, "12345", "12345"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("6 đến 127");
    }

    @Test
    void doiMatKhauNhanVien_XacNhanKhongKhop_BiTuChoi() {
        assertThatThrownBy(() -> service.doiMatKhauNhanVien(NV_ID, "matKhauMoi123", "khacNhau123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("xác nhận không khớp");
    }

    @Test
    void doiMatKhauNhanVien_KhongTimThay_BiTuChoi() {
        when(nhanVienRepository.findById(NV_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.doiMatKhauNhanVien(NV_ID, "matKhauMoi123", "matKhauMoi123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không tìm thấy nhân viên");
    }

    @Test
    void doiMatKhauKhachHang_ThanhCong_LuuBcrypt() {
        KhachHang kh = khachHang();
        when(khachHangRepository.findById(KH_ID)).thenReturn(Optional.of(kh));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));

        service.doiMatKhauKhachHang(KH_ID, "matKhauMoi123", "matKhauMoi123");

        ArgumentCaptor<KhachHang> captor = ArgumentCaptor.forClass(KhachHang.class);
        verify(khachHangRepository).save(captor.capture());
        String daLuu = captor.getValue().getMatKhau();
        assertThat(passwordEncoder.matches("matKhauMoi123", daLuu)).isTrue();
    }

    @Test
    void doiMatKhauKhachHang_KhongTimThay_BiTuChoi() {
        when(khachHangRepository.findById(KH_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.doiMatKhauKhachHang(KH_ID, "matKhauMoi123", "matKhauMoi123"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Không tìm thấy khách hàng");
    }
}
