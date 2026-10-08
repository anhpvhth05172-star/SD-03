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
import com.example.datn.dto.AuthResponse;
import com.example.datn.dto.DangNhapRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.PhienDangNhap;
import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.PhienDangNhapRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

class PhienDangNhapServiceTest {

    private static final String JWT_SECRET = "VGVzdC1KV1QtU2VjcmV0LUtleS0wMTIzNDU2Nzg5QUJDREVGR0g=";
    private static final long ACCESS_TTL_MS = 900_000L;
    private static final long PHIEN_TTL_MS = 1_800_000L;

    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final PhienDangNhapRepository phienDangNhapRepository = mock(PhienDangNhapRepository.class);
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final JwtService jwtService = new JwtService(JWT_SECRET, ACCESS_TTL_MS);
    private final AuthService service = new AuthService(
        khachHangRepository,
        passwordEncoder,
        jwtService,
        phienDangNhapRepository,
        PHIEN_TTL_MS
    );

    private KhachHang taoKhachHang() {
        KhachHang kh = new KhachHang();
        kh.setId(5L);
        kh.setTenTaiKhoan("nguyenvana");
        kh.setEmail("nguyenvana@example.com");
        kh.setTenKhachHang("nguyenvana");
        kh.setMatKhau(passwordEncoder.encode("matkhau123"));
        kh.setTrangThai(true);
        return kh;
    }

    private PhienDangNhap taoPhien(long id, long idKhachHang, LocalDateTime hoatDongCuoi, boolean revoked) {
        PhienDangNhap phien = new PhienDangNhap();
        phien.setId(id);
        phien.setIdKhachHang(idKhachHang);
        phien.setRefreshTokenHash("hash-moi");
        phien.setNgayTao(hoatDongCuoi);
        phien.setHoatDongCuoi(hoatDongCuoi);
        phien.setRevoked(revoked);
        return phien;
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
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ===== Dang nhap: tao Access + Refresh Token + phien =====

    @Test
    void dangNhap_taoAccessVaRefreshToken_vaTaoPhien() {
        when(khachHangRepository.findByEmailOrTenTaiKhoan("nguyenvana")).thenReturn(Optional.of(taoKhachHang()));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> {
            PhienDangNhap p = inv.getArgument(0);
            p.setId(77L);
            return p;
        });

        AuthResponse resp = service.dangNhap(new DangNhapRequest("nguyenvana", "matkhau123"));

        assertThat(resp.getToken()).isNotBlank();
        assertThat(resp.getRefreshToken()).isNotBlank();
        assertThat(resp.getRefreshToken()).isNotEqualTo(resp.getToken());
        assertThat(jwtService.hopLe(resp.getToken())).isTrue();
        assertThat(jwtService.layIdKhachHang(resp.getToken())).isEqualTo(5L);
        assertThat(jwtService.layIdPhien(resp.getToken())).isEqualTo(77L);

        ArgumentCaptor<PhienDangNhap> cap = ArgumentCaptor.forClass(PhienDangNhap.class);
        verify(phienDangNhapRepository).save(cap.capture());
        PhienDangNhap phien = cap.getValue();
        assertThat(phien.getIdKhachHang()).isEqualTo(5L);
        assertThat(phien.getRefreshTokenHash()).hasSize(64);
        assertThat(phien.getRefreshTokenHash()).isNotEqualTo(resp.getRefreshToken());
        assertThat(phien.getRefreshTokenHash()).isEqualTo(sha256(resp.getRefreshToken()));
        assertThat(phien.isRevoked()).isFalse();
        assertThat(phien.getHoatDongCuoi()).isAfter(LocalDateTime.now().minusMinutes(1));
    }

    @Test
    void dangNhapSaiMatKhau_khongTaoPhien() {
        KhachHang kh = taoKhachHang();
        when(khachHangRepository.findByEmailOrTenTaiKhoan("nguyenvana")).thenReturn(Optional.of(kh));

        assertThatThrownBy(() -> service.dangNhap(new DangNhapRequest("nguyenvana", "saimatkhau")))
            .isInstanceOf(DangNhapThatBaiException.class);
        verify(phienDangNhapRepository, never()).save(any());
    }

    // ===== Refresh =====

    @Test
    void refresh_hopLe_capTokenMoi_vaGiaHanHoatDongCuoi() {
        KhachHang kh = taoKhachHang();
        LocalDateTime hoatDongCuoiCu = LocalDateTime.now().minusMinutes(10);
        PhienDangNhap phien = taoPhien(77L, 5L, hoatDongCuoiCu, false);
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));
        when(khachHangRepository.findById(5L)).thenReturn(Optional.of(kh));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.refresh("refresh-token-cu");

        assertThat(jwtService.hopLe(resp.getToken())).isTrue();
        assertThat(jwtService.layIdKhachHang(resp.getToken())).isEqualTo(5L);
        assertThat(jwtService.layIdPhien(resp.getToken())).isEqualTo(77L);
        assertThat(resp.getRefreshToken()).isEqualTo("refresh-token-cu");
        assertThat(resp.getTenTaiKhoan()).isEqualTo("nguyenvana");
        assertThat(phien.getHoatDongCuoi()).isAfter(hoatDongCuoiCu);
        verify(phienDangNhapRepository).save(phien);
    }

    @Test
    void refresh_accessHetHan_NhungPhienVanCon_LayDuocTokenMoi() {
        JwtService accessHetHanService = new JwtService(JWT_SECRET, -1_000L);
        KhachHang kh = taoKhachHang();
        String accessCu = accessHetHanService.taoToken(kh, 77L);
        assertThat(jwtService.hopLe(accessCu)).isFalse();

        PhienDangNhap phien = taoPhien(77L, 5L, LocalDateTime.now(), false);
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));
        when(khachHangRepository.findById(5L)).thenReturn(Optional.of(kh));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.refresh("refresh-token-cu");

        assertThat(jwtService.hopLe(resp.getToken())).isTrue();
        assertThat(jwtService.layIdPhien(resp.getToken())).isEqualTo(77L);
    }

    @Test
    void refresh_khiPhienDaQua30PhutKhongHoatDong_biTuChoi() {
        PhienDangNhap phien = taoPhien(77L, 5L, LocalDateTime.now().minusMinutes(31), false);
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));

        assertThatThrownBy(() -> service.refresh("refresh-token-cu"))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessageContaining("hết hạn");
        verify(phienDangNhapRepository, never()).save(any());
    }

    @Test
    void refresh_khiPhienConTrong29Phut_vanChapNhan() {
        KhachHang kh = taoKhachHang();
        PhienDangNhap phien = taoPhien(77L, 5L, LocalDateTime.now().minusMinutes(29), false);
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));
        when(khachHangRepository.findById(5L)).thenReturn(Optional.of(kh));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.refresh("refresh-token-cu");

        assertThat(jwtService.hopLe(resp.getToken())).isTrue();
    }

    @Test
    void refresh_khiKhongTimThayPhien_biTuChoi() {
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.refresh("token-khong-ton-tai"))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessageContaining("không hợp lệ");
    }

    @Test
    void refresh_khiTokenRong_biTuChoi() {
        assertThatThrownBy(() -> service.refresh(null))
            .isInstanceOf(DangNhapThatBaiException.class);
        assertThatThrownBy(() -> service.refresh("  "))
            .isInstanceOf(DangNhapThatBaiException.class);
    }

    @Test
    void refresh_phienGanVoiKhachHang_dungChuSoHuu() {
        PhienDangNhap phien = taoPhien(77L, 99L, LocalDateTime.now(), false);
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));
        KhachHang kh = taoKhachHang();
        kh.setId(99L);
        when(khachHangRepository.findById(99L)).thenReturn(Optional.of(kh));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> inv.getArgument(0));

        AuthResponse resp = service.refresh("refresh-token-cu");

        assertThat(resp.getIdKhachHang()).isEqualTo(99L);
        assertThat(jwtService.layIdKhachHang(resp.getToken())).isEqualTo(99L);
        assertThat(jwtService.layIdPhien(resp.getToken())).isEqualTo(77L);
    }

    // ===== Dang xuat / revoke =====

    @Test
    void dangXuat_revokePhien_refreshCuBiTuChoi() {
        PhienDangNhap phien = taoPhien(77L, 5L, LocalDateTime.now(), false);
        when(phienDangNhapRepository.findByRefreshTokenHash(anyString())).thenReturn(Optional.of(phien));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> inv.getArgument(0));

        service.dangXuat("refresh-token-cu");

        assertThat(phien.isRevoked()).isTrue();
        assertThat(phien.getNgayDangXuat()).isNotNull();
        verify(phienDangNhapRepository).save(phien);

        assertThatThrownBy(() -> service.refresh("refresh-token-cu"))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessageContaining("hết hạn");
    }

    @Test
    void dangXuat_khongCoToken_khongXuLyGi() {
        service.dangXuat(null);
        service.dangXuat("   ");
        verify(phienDangNhapRepository, never()).save(any());
        verify(phienDangNhapRepository, never()).findByRefreshTokenHash(anyString());
    }

    // ===== Quy tac 30 phut (entity) =====

    @Test
    void hetHan_tinhTheoHoatDongCuoi_khongPhaiTheoNgayTao() {
        LocalDateTime bayGio = LocalDateTime.now();
        PhienDangNhap phien = taoPhien(1L, 5L, bayGio.minusMinutes(29), false);

        assertThat(phien.hetHan(bayGio, PHIEN_TTL_MS)).isFalse();
        assertThat(phien.hetHan(bayGio.plusMinutes(2), PHIEN_TTL_MS)).isTrue();

        PhienDangNhap daGiaHan = taoPhien(2L, 5L, bayGio.minusMinutes(1), false);
        assertThat(daGiaHan.hetHan(bayGio.plusMinutes(29), PHIEN_TTL_MS)).isFalse();
        assertThat(daGiaHan.hetHan(bayGio.plusMinutes(31), PHIEN_TTL_MS)).isTrue();
    }
}
