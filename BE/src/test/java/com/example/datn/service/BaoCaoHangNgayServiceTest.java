package com.example.datn.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.dto.ThongKe.TrangThaiDonDTO;
import com.example.datn.entity.NhanVien;
import com.example.datn.repository.NhanVienRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BaoCaoHangNgayServiceTest {

    private NhanVienRepository nhanVienRepository;
    private ThongKeService thongKeService;
    private EmailService emailService;
    private BaoCaoHangNgayService service;

    @BeforeEach
    void setUp() {
        nhanVienRepository = mock(NhanVienRepository.class);
        thongKeService = mock(ThongKeService.class);
        emailService = mock(EmailService.class);
        service = new BaoCaoHangNgayService(nhanVienRepository, thongKeService, emailService);
    }

    @Test
    void guiBaoCaoGuiThongKeNgayDuocChonDenEmailAdmin() {
        LocalDate ngay = LocalDate.of(2026, 10, 27);
        NhanVien admin = new NhanVien();
        admin.setEmail("admin@example.com");
        when(nhanVienRepository.findFirstByVaiTro_MaVaiTroAndTrangThaiTrueAndEmailIsNotNullOrderByIdAsc("ADMIN"))
            .thenReturn(Optional.of(admin));
        when(thongKeService.tongQuan(ngay, ngay, null))
            .thenReturn(new TongQuanDTO(1200000, 700000, 500000, 3, 400000, 4, 1));
        when(thongKeService.trangThaiDon(ngay, ngay, null))
            .thenReturn(List.of(new TrangThaiDonDTO("Chờ xác nhận", 2)));

        service.guiBaoCao(ngay);

        verify(emailService).guiMailCustom(
            eq("admin@example.com"),
            eq("Báo cáo kinh doanh PolyShoes - 27/10/2026"),
            contains("1.200.000")
        );
    }

    @Test
    void boQuaGuiKhiChuaCoEmailAdmin() {
        when(nhanVienRepository.findFirstByVaiTro_MaVaiTroAndTrangThaiTrueAndEmailIsNotNullOrderByIdAsc("ADMIN"))
            .thenReturn(Optional.empty());

        service.guiBaoCao(LocalDate.of(2026, 10, 27));

        verify(emailService, never()).guiMailCustom(any(), any(), any());
    }
}
