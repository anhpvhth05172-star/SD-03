package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.datn.config.JwtService;
import com.example.datn.dto.Auth.AuthResponse;
import com.example.datn.dto.Auth.DangNhapRequest;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhienDangNhap;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhienDangNhapRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

class GhiNhoDangNhapTest {

    private static final long PHIEN_THONG_THUONG_MS = 8 * 3600_000L;
    private static final long PHIEN_GHI_NHO_MS = 30L * 24 * 3600_000;

    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);
    private final PhienDangNhapRepository phienDangNhapRepository = mock(PhienDangNhapRepository.class);
    private final JwtService jwtService = mock(JwtService.class);

    private final AuthService service = new AuthService(
        khachHangRepository,
        nhanVienRepository,
        new BCryptPasswordEncoder(),
        jwtService,
        phienDangNhapRepository,
        PHIEN_THONG_THUONG_MS,
        mock(EmailService.class),
        new QuenMatKhauOtpService(),
        PHIEN_GHI_NHO_MS
    );

    private NhanVien nhanVien() {
        NhanVien nv = new NhanVien();
        nv.setId(4L);
        nv.setTenTaiKhoan("duc");
        nv.setEmail("duc@gmail.com");
        nv.setMatKhau("123456");
        nv.setTrangThai(true);
        VaiTro vaiTro = new VaiTro();
        vaiTro.setId(3L);
        vaiTro.setMaVaiTro("STAFF");
        nv.setVaiTro(vaiTro);
        return nv;
    }

    private DangNhapRequest yeuCau(Boolean ghiNho) {
        DangNhapRequest req = new DangNhapRequest();
        req.setTaiKhoan("duc");
        req.setMatKhau("123456");
        req.setGhiNhoDangNhap(ghiNho);
        return req;
    }

    private void chuanBiDangNhap() {
        when(nhanVienRepository.findByEmailOrTenTaiKhoan("duc")).thenReturn(Optional.of(nhanVien()));
        when(nhanVienRepository.findById(4L)).thenReturn(Optional.of(nhanVien()));
        when(nhanVienRepository.save(any(NhanVien.class))).thenAnswer(inv -> inv.getArgument(0));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> inv.getArgument(0));
        when(jwtService.taoTokenNhanVien(any(NhanVien.class), any())).thenReturn("jwt-moi");
    }

    @Test
    void dangNhapKhongGhiNho_RefreshTokenBinhThuong() {
        chuanBiDangNhap();

        AuthResponse resp = service.dangNhapNhanVien(yeuCau(false));

        assertThat(resp.getRefreshToken()).isNotBlank().doesNotStartWith("r_");
    }

    @Test
    void dangNhapGhiNho_RefreshTokenCoPrefixR() {
        chuanBiDangNhap();

        AuthResponse resp = service.dangNhapNhanVien(yeuCau(true));

        assertThat(resp.getRefreshToken()).startsWith("r_");
    }

    @Test
    void refreshPhienThongThuong_VanHopLeSau7Gio() {
        chuanBiDangNhap();
        AuthResponse resp = service.dangNhapNhanVien(yeuCau(false));
        ArgumentCaptor<PhienDangNhap> captor = ArgumentCaptor.forClass(PhienDangNhap.class);
        org.mockito.Mockito.verify(phienDangNhapRepository).save(captor.capture());
        PhienDangNhap phien = captor.getValue();
        phien.setHoatDongCuoi(LocalDateTime.now().minusHours(7));
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));

        AuthResponse moi = service.refresh(resp.getRefreshToken());

        assertThat(moi.getToken()).isEqualTo("jwt-moi");
    }

    @Test
    void refreshPhienThongThuong_HetHanSau9Gio() {
        chuanBiDangNhap();
        AuthResponse resp = service.dangNhapNhanVien(yeuCau(false));
        ArgumentCaptor<PhienDangNhap> captor = ArgumentCaptor.forClass(PhienDangNhap.class);
        org.mockito.Mockito.verify(phienDangNhapRepository).save(captor.capture());
        PhienDangNhap phien = captor.getValue();
        phien.setHoatDongCuoi(LocalDateTime.now().minusHours(9));
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));

        assertThatThrownBy(() -> service.refresh(resp.getRefreshToken()))
            .isInstanceOf(com.example.datn.exception.DangNhapThatBaiException.class)
            .hasMessageContaining("hết hạn");
    }

    @Test
    void refreshGhiNho_VanHopLeSau9Gio() {
        chuanBiDangNhap();
        AuthResponse resp = service.dangNhapNhanVien(yeuCau(true));
        ArgumentCaptor<PhienDangNhap> captor = ArgumentCaptor.forClass(PhienDangNhap.class);
        org.mockito.Mockito.verify(phienDangNhapRepository).save(captor.capture());
        PhienDangNhap phien = captor.getValue();
        phien.setHoatDongCuoi(LocalDateTime.now().minusHours(9));
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));

        AuthResponse moi = service.refresh(resp.getRefreshToken());

        assertThat(moi.getToken()).isEqualTo("jwt-moi");
    }

    @Test
    void refreshGhiNho_HetHanSau31Ngay() {
        chuanBiDangNhap();
        AuthResponse resp = service.dangNhapNhanVien(yeuCau(true));
        ArgumentCaptor<PhienDangNhap> captor = ArgumentCaptor.forClass(PhienDangNhap.class);
        org.mockito.Mockito.verify(phienDangNhapRepository).save(captor.capture());
        PhienDangNhap phien = captor.getValue();
        phien.setHoatDongCuoi(LocalDateTime.now().minusDays(31));
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));

        assertThatThrownBy(() -> service.refresh(resp.getRefreshToken()))
            .isInstanceOf(com.example.datn.exception.DangNhapThatBaiException.class)
            .hasMessageContaining("hết hạn");
    }
}
