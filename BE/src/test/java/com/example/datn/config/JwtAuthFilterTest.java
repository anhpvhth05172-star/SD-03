package com.example.datn.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.entity.KhachHang;
import com.example.datn.entity.PhienDangNhap;
import com.example.datn.repository.PhienDangNhapRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class JwtAuthFilterTest {

    private static final String JWT_SECRET = "VGVzdC1KV1QtU2VjcmV0LUtleS0wMTIzNDU2Nzg5QUJDREVGR0g=";
    private static final long ACCESS_TTL_MS = 900_000L;
    private static final long PHIEN_TTL_MS = 1_800_000L;

    private final JwtService jwtService = new JwtService(JWT_SECRET, ACCESS_TTL_MS);
    private final PhienDangNhapRepository phienDangNhapRepository = mock(PhienDangNhapRepository.class);
    private final JwtAuthFilter filter = new JwtAuthFilter(jwtService, phienDangNhapRepository, PHIEN_TTL_MS);

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    private KhachHang taoKhachHang() {
        KhachHang kh = new KhachHang();
        kh.setId(5L);
        kh.setTenTaiKhoan("nguyenvana");
        return kh;
    }

    private PhienDangNhap taoPhien(long idKhachHang, LocalDateTime hoatDongCuoi, boolean revoked) {
        PhienDangNhap phien = new PhienDangNhap();
        phien.setId(77L);
        phien.setIdKhachHang(idKhachHang);
        phien.setRefreshTokenHash("hash");
        phien.setNgayTao(hoatDongCuoi);
        phien.setHoatDongCuoi(hoatDongCuoi);
        phien.setRevoked(revoked);
        return phien;
    }

    private Authentication chay(String token) throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (token != null) {
            request.addHeader("Authorization", "Bearer " + token);
        }
        filter.doFilter(request, new MockHttpServletResponse(), new MockFilterChain());
        return SecurityContextHolder.getContext().getAuthentication();
    }

    @Test
    void tokenHopLe_vaPhienHopLe_authenticate_vaGiaHanHoatDong() throws Exception {
        LocalDateTime hoatDongCuoiCu = LocalDateTime.now().minusMinutes(20);
        PhienDangNhap phien = taoPhien(5L, hoatDongCuoiCu, false);
        when(phienDangNhapRepository.findById(77L)).thenReturn(Optional.of(phien));
        when(phienDangNhapRepository.save(any(PhienDangNhap.class))).thenAnswer(inv -> inv.getArgument(0));
        String token = jwtService.taoToken(taoKhachHang(), 77L);

        Authentication auth = chay(token);

        assertThat(auth).isNotNull();
        assertThat(auth.getPrincipal()).isEqualTo(5L);
        assertThat(phien.getHoatDongCuoi()).isAfter(hoatDongCuoiCu);
        verify(phienDangNhapRepository).save(phien);
    }

    @Test
    void tokenHopLe_vaPhienHetHan_khongAuthenticate() throws Exception {
        PhienDangNhap phien = taoPhien(5L, LocalDateTime.now().minusMinutes(31), false);
        when(phienDangNhapRepository.findById(77L)).thenReturn(Optional.of(phien));
        String token = jwtService.taoToken(taoKhachHang(), 77L);

        Authentication auth = chay(token);

        assertThat(auth).isNull();
        verify(phienDangNhapRepository, never()).save(any());
    }

    @Test
    void tokenHopLe_vaPhienDaLogout_khongAuthenticate() throws Exception {
        PhienDangNhap phien = taoPhien(5L, LocalDateTime.now(), true);
        when(phienDangNhapRepository.findById(77L)).thenReturn(Optional.of(phien));
        String token = jwtService.taoToken(taoKhachHang(), 77L);

        Authentication auth = chay(token);

        assertThat(auth).isNull();
    }

    @Test
    void tokenHetHan_khongAuthenticate() throws Exception {
        JwtService accessHetHanService = new JwtService(JWT_SECRET, -1_000L);
        PhienDangNhap phien = taoPhien(5L, LocalDateTime.now(), false);
        when(phienDangNhapRepository.findById(77L)).thenReturn(Optional.of(phien));
        String tokenHetHan = accessHetHanService.taoToken(taoKhachHang(), 77L);

        Authentication auth = chay(tokenHetHan);

        assertThat(auth).isNull();
        verify(phienDangNhapRepository, never()).findById(any());
    }

    @Test
    void tokenCuKhongCoPhienId_khongAuthenticate() throws Exception {
        String tokenCu = jwtService.taoToken(taoKhachHang(), null);

        Authentication auth = chay(tokenCu);

        assertThat(auth).isNull();
        verify(phienDangNhapRepository, never()).findById(any());
    }

    @Test
    void khongCoHeader_khongAuthenticate_vaVanChayChain() throws Exception {
        Authentication auth = chay(null);
        assertThat(auth).isNull();
    }

    @Test
    void phienCuaKhachHangKhac_khongAuthenticate() throws Exception {
        PhienDangNhap phien = taoPhien(99L, LocalDateTime.now(), false);
        when(phienDangNhapRepository.findById(77L)).thenReturn(Optional.of(phien));
        String token = jwtService.taoToken(taoKhachHang(), 77L);

        Authentication auth = chay(token);

        assertThat(auth).isNull();
        verify(phienDangNhapRepository, never()).save(any());
    }
}
