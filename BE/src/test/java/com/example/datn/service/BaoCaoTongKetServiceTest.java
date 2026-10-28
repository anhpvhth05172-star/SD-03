package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.ThongKe.GuiMailTongKetRequest;
import com.example.datn.dto.ThongKe.GuiMailTongKetResponse;
import com.example.datn.dto.ThongKe.KySoSanhDTO;
import com.example.datn.dto.ThongKe.SoSanhDTO;
import com.example.datn.dto.ThongKe.TopSanPhamKyDTO;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.entity.NhanVien;
import com.example.datn.repository.NhanVienRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class BaoCaoTongKetServiceTest {

    private ThongKeService thongKeService;
    private EmailService emailService;
    private NhanVienRepository nhanVienRepository;
    private BaoCaoTongKetService service;

    @BeforeEach
    void setUp() {
        thongKeService = mock(ThongKeService.class);
        emailService = mock(EmailService.class);
        nhanVienRepository = mock(NhanVienRepository.class);
        service = new BaoCaoTongKetService(thongKeService, emailService, nhanVienRepository, "no-reply@polyshoes.vn");
    }

    private static TongQuanDTO tongQuan(long doanhThu, long soDon) {
        return new TongQuanDTO(doanhThu, doanhThu / 2, doanhThu - doanhThu / 2, soDon,
            soDon > 0 ? doanhThu / soDon : 0, 10, 0);
    }

    private static KySoSanhDTO ky(String maKy, String tenKy, long doanhThu, int tangTruong) {
        return new KySoSanhDTO(maKy, tenKy, LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31),
            doanhThu, 0, 0, 0, 0, 0, 0, tangTruong, false);
    }

    private static TopSanPhamKyDTO top(String ma, String ten, long ban, long dt) {
        return new TopSanPhamKyDTO(ma, ten, "Nike", ban, dt, List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L, 11L, ban));
    }

    private void mockSoLieuThang() {
        // tongQuan goi 2 lan: ky hien tai truoc, ky truoc sau
        when(thongKeService.tongQuan(any(), any(), any()))
            .thenReturn(tongQuan(1_000_000L, 10), tongQuan(800_000L, 8));
        when(thongKeService.demDonHuy(any(), any())).thenReturn(3L);
        List<KySoSanhDTO> ky = List.of(
            ky("T1", "Tháng 1", 0, 0), ky("T2", "Tháng 2", 0, 0), ky("T3", "Tháng 3", 0, 0),
            ky("T4", "Tháng 4", 0, 0), ky("T5", "Tháng 5", 0, 0), ky("T6", "Tháng 6", 0, 0),
            ky("T7", "Tháng 7", 0, 0), ky("T8", "Tháng 8", 0, 0), ky("T9", "Tháng 9", 700_000, 0),
            ky("T10", "Tháng 10", 1_000_000, 43), ky("T11", "Tháng 11", 0, -100), ky("T12", "Tháng 12", 0, 0)
        );
        List<TopSanPhamKyDTO> top6 = List.of(
            top("SP001", "Giay A", 50, 5_000_000),
            top("SP002", "Giay B", 40, 4_000_000),
            top("SP003", "Giay C", 30, 3_000_000),
            top("SP004", "Giay D", 20, 2_000_000),
            top("SP005", "Giay E", 10, 1_000_000),
            top("SP006", "Giay F", 5, 500_000)
        );
        when(thongKeService.soSanh(any(), any(), any(), any(), any(), any(), any(), any()))
            .thenReturn(new SoSanhDTO(ky, top6));
    }

    @Test
    void guiMailTongKet_thangHopLeGuiBangSoLieuThat() {
        mockSoLieuThang();
        when(emailService.guiMailCustom(any(), any(), any())).thenReturn(true);

        GuiMailTongKetResponse res = service.guiMailTongKet(
            new GuiMailTongKetRequest("THANG", 10, 2026, "admin@polyshoes.vn"));

        assertTrue(res.thanhCong());
        assertEquals("admin@polyshoes.vn", res.emailNhan());
        assertNull(res.lyDo());

        ArgumentCaptor<String> toCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> tieuDeCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<String> noiDungCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).guiMailCustom(toCaptor.capture(), tieuDeCaptor.capture(), noiDungCaptor.capture());
        assertEquals("admin@polyshoes.vn", toCaptor.getValue());
        assertTrue(tieuDeCaptor.getValue().contains("Tháng 10 Năm 2026"), tieuDeCaptor.getValue());
        String body = noiDungCaptor.getValue();
        assertTrue(body.contains("TỔNG DOANH THU"), body);
        assertTrue(body.contains("TOP 5"), body);
        assertTrue(body.contains("BẢNG ĐỐI CHIẾU"), body);
        // top5 cat tu top6
        assertTrue(body.contains("Giay A"), body);
        assertFalse(body.contains("Giay F"), body);
    }

    @Test
    void guiMailTongKet_mailerTraFalseTraVeThanhCongFalseVaLyDo() {
        mockSoLieuThang();
        when(emailService.guiMailCustom(any(), any(), any())).thenReturn(false);

        GuiMailTongKetResponse res = service.guiMailTongKet(
            new GuiMailTongKetRequest("THANG", 10, 2026, "admin@polyshoes.vn"));

        assertFalse(res.thanhCong());
        assertNotEquals("", res.lyDo() == null ? "" : res.lyDo());
    }

    @Test
    void guiMailTongKet_loaiSaiThangSaiEmailSaiNemIAE() {
        assertThrows(IllegalArgumentException.class, () ->
            service.guiMailTongKet(new GuiMailTongKetRequest("QUY", 10, 2026, "a@b.vn")));
        assertThrows(IllegalArgumentException.class, () ->
            service.guiMailTongKet(new GuiMailTongKetRequest("THANG", 13, 2026, "a@b.vn")));
        assertThrows(IllegalArgumentException.class, () ->
            service.guiMailTongKet(new GuiMailTongKetRequest("THANG", 10, 2026, "sai-email")));
    }

    @Test
    void emailMacDinhLayEmailAdminDauTien() {
        NhanVien nv = new NhanVien();
        nv.setMaNhanVien("NV001");
        nv.setEmail("admin@polyshoes.vn");
        when(nhanVienRepository.findFirstByVaiTro_MaVaiTroAndTrangThaiTrueAndEmailIsNotNullOrderByMaNhanVienAsc("ADMIN"))
            .thenReturn(Optional.of(nv));

        assertEquals("admin@polyshoes.vn", service.emailMacDinh());
    }

    @Test
    void emailMacDinhFallbackSangMailUsernameKhiKhongCoAdmin() {
        when(nhanVienRepository.findFirstByVaiTro_MaVaiTroAndTrangThaiTrueAndEmailIsNotNullOrderByMaNhanVienAsc("ADMIN"))
            .thenReturn(Optional.empty());

        assertEquals("no-reply@polyshoes.vn", service.emailMacDinh());
    }
}
