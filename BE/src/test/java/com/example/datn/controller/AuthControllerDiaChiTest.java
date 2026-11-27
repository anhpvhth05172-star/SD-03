package com.example.datn.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.Auth.CapNhatThongTinKhachRequest;
import com.example.datn.dto.Auth.ThongTinResponse;
import com.example.datn.dto.KhachHang.DiaChiKhachHangDTO;
import com.example.datn.dto.KhachHang.DiaChiKhachHangRequest;
import com.example.datn.dto.KhachHang.KhachHangDTO;
import com.example.datn.entity.KhachHang;
import com.example.datn.exception.DangNhapThatBaiException;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.service.AuthService;
import com.example.datn.service.KhachHangService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

class AuthControllerDiaChiTest {

    private final AuthService authService = mock(AuthService.class);
    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);
    private final KhachHangService khachHangService = mock(KhachHangService.class);

    private final AuthController controller = new AuthController(
        authService,
        khachHangRepository,
        nhanVienRepository,
        khachHangService
    );

    @BeforeEach
    void setUpCustomer() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                43L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_KHACH_HANG"))
            )
        );
        KhachHang kh = new KhachHang();
        kh.setId(43L);
        kh.setTenTaiKhoan("kh_test");
        kh.setTenKhachHang("Khách Test");
        kh.setEmail("kh@test.vn");
        kh.setSoDienThoai("0901234567");
        kh.setVaiTro("KHACH_HANG");
        when(khachHangRepository.findById(43L)).thenReturn(Optional.of(kh));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));

        DiaChiKhachHangDTO dc = new DiaChiKhachHangDTO(
            10L, 43L, "DC01", "Nhà riêng", "Thành phố Hà Nội", "Phường Ba Đình", "123 Cầu Giấy", true, true
        );
        KhachHangDTO dto = new KhachHangDTO();
        dto.setMaKhachHang("KH043");
        dto.setDiaChiMacDinh(dc);
        dto.setDanhSachDiaChi(List.of(dc));
        when(khachHangService.get(43L)).thenReturn(dto);
    }

    @AfterEach
    void clearSecurity() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void capNhatThongTin_idDiaChiMacDinh_goiDatMacDinhVaTraThongTin() {
        CapNhatThongTinKhachRequest req = new CapNhatThongTinKhachRequest();
        req.setIdDiaChiMacDinh(10L);

        ThongTinResponse res = controller.capNhatThongTin(req);

        verify(khachHangService).datMacDinh(43L, 10L);
        verify(khachHangService, never()).themDiaChi(any(), any());
        verify(khachHangService, never()).capNhatDiaChi(any(), any(), any());
        assertThat(res.getDanhSachDiaChi()).hasSize(1);
        assertThat(res.getDiaChiMacDinh().id()).isEqualTo(10L);
    }

    @Test
    void capNhatThongTin_themVaSuaDiaChi_goiDungServiceTheoIdDiaChi() {
        CapNhatThongTinKhachRequest createReq = new CapNhatThongTinKhachRequest();
        createReq.setTenDiaChi("Nhà mới");
        createReq.setTinhThanhPho("Thành phố Hà Nội");
        createReq.setPhuong("Phường Ba Đình");
        createReq.setDiaChiCuThe("123 Cầu Giấy");
        createReq.setMacDinh(true);

        controller.capNhatThongTin(createReq);
        verify(khachHangService).themDiaChi(eq(43L), any(DiaChiKhachHangRequest.class));

        CapNhatThongTinKhachRequest updateReq = new CapNhatThongTinKhachRequest();
        updateReq.setIdDiaChi(10L);
        updateReq.setTenDiaChi("Nhà sửa");
        updateReq.setTinhThanhPho("Thành phố Hà Nội");
        updateReq.setPhuong("Phường Ba Đình");
        updateReq.setDiaChiCuThe("456 Cầu Giấy");
        updateReq.setMacDinh(false);

        controller.capNhatThongTin(updateReq);
        verify(khachHangService).capNhatDiaChi(eq(43L), eq(10L), any(DiaChiKhachHangRequest.class));
    }

    @Test
    void xoaDiaChiCuaToi_khachHangHopLe_goiXoaVaTraThongTin() {
        ThongTinResponse res = controller.xoaDiaChiCuaToi(10L);

        verify(khachHangService).xoaDiaChi(43L, 10L);
        assertThat(res.getId()).isEqualTo(43L);
    }

    @Test
    void xoaDiaChiCuaToi_nhanVien_tuChoiVaKhongGoiService() {
        SecurityContextHolder.getContext().setAuthentication(
            new UsernamePasswordAuthenticationToken(
                271L,
                null,
                List.of(new SimpleGrantedAuthority("ROLE_NHAN_VIEN"))
            )
        );

        assertThatThrownBy(() -> controller.xoaDiaChiCuaToi(10L))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("Chỉ tài khoản khách hàng mới hỗ trợ xóa địa chỉ");
        verify(khachHangService, never()).xoaDiaChi(any(), any());
    }

    @Test
    void xoaDiaChiCuaToi_chuaDangNhap_nemDangNhapThatBai() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> controller.xoaDiaChiCuaToi(10L))
            .isInstanceOf(DangNhapThatBaiException.class)
            .hasMessage("Chưa đăng nhập");
        verify(khachHangService, never()).xoaDiaChi(any(), any());
    }
}
