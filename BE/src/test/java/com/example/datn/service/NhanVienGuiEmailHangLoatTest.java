package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.NhanVien.GuiEmailHangLoatRequest;
import com.example.datn.dto.NhanVien.GuiEmailHangLoatResponse;
import com.example.datn.entity.NhanVien;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.PlatformTransactionManager;

class NhanVienGuiEmailHangLoatTest {

    private static NhanVienRepository repoVoiCacNhanVien(NhanVien... nhanViens) {
        NhanVienRepository repo = mock(NhanVienRepository.class);
        when(repo.findAllById(any())).thenReturn(List.of(nhanViens));
        return repo;
    }

    private static NhanVien nhanVien(Long id, String ma, String ten, String email) {
        NhanVien nv = new NhanVien();
        nv.setId(id);
        nv.setMaNhanVien(ma);
        nv.setTenTaiKhoan(ten);
        nv.setEmail(email);
        return nv;
    }

    private static NhanVienService service(NhanVienRepository repo, EmailService emailService) {
        return new NhanVienService(
            repo,
            mock(VaiTroRepository.class),
            mock(PlatformTransactionManager.class),
            emailService
        );
    }

    private static GuiEmailHangLoatRequest yeuCau(List<Long> ids) {
        return new GuiEmailHangLoatRequest(ids, "Thông báo từ PolyShoes", "Nội dung thông báo.");
    }

    @Test
    void guiThanhCongVaBoQuaNhanVienKhongEmail() {
        NhanVien nv1 = nhanVien(1L, "Anhph", "Phạm Hà Anh", "nv1@example.com");
        NhanVien nv2 = nhanVien(2L, "Binhnt", "Nguyễn Thị Bình", null);
        EmailService emailService = mock(EmailService.class);
        when(emailService.guiMailCustom(eq("nv1@example.com"), anyString(), anyString()))
            .thenReturn(true);

        GuiEmailHangLoatResponse res = service(repoVoiCacNhanVien(nv1, nv2), emailService)
            .guiEmailHangLoat(yeuCau(List.of(1L, 2L)));

        assertEquals(2, res.getTongYeuCau());
        assertEquals(1, res.getGuiThanhCong());
        assertEquals(1, res.getThatBai());
        assertEquals(1, res.getBoQua().size());
        assertEquals(2L, res.getBoQua().get(0).getId());
        assertTrue(res.getBoQua().get(0).getLyDo().contains("email"));
        verify(emailService).guiMailCustom(eq("nv1@example.com"), anyString(), anyString());
    }

    @Test
    void nhaCungCapTuChoi_DuocGhiNhanThatBai() {
        NhanVien nv = nhanVien(1L, "Anhph", "Phạm Hà Anh", "nv1@example.com");
        EmailService emailService = mock(EmailService.class);
        when(emailService.guiMailCustom(anyString(), anyString(), anyString())).thenReturn(false);

        GuiEmailHangLoatResponse res = service(repoVoiCacNhanVien(nv), emailService)
            .guiEmailHangLoat(yeuCau(List.of(1L)));

        assertEquals(0, res.getGuiThanhCong());
        assertEquals(1, res.getThatBai());
        assertEquals(1, res.getBoQua().size());
    }

    @Test
    void khongTimThayNhanVien_DuocGhiNhanLyDo() {
        NhanVienRepository repo = mock(NhanVienRepository.class);
        when(repo.findAllById(any())).thenReturn(List.of());
        EmailService emailService = mock(EmailService.class);

        GuiEmailHangLoatResponse res = service(repo, emailService)
            .guiEmailHangLoat(yeuCau(List.of(99L)));

        assertEquals(1, res.getThatBai());
        assertTrue(res.getBoQua().get(0).getLyDo().contains("Không tìm thấy nhân viên"));
    }

    @Test
    void thieuTieuDe_BaoLoi() {
        NhanVienService service = service(repoVoiCacNhanVien(), mock(EmailService.class));
        assertThrows(IllegalArgumentException.class,
            () -> service.guiEmailHangLoat(new GuiEmailHangLoatRequest(List.of(1L), "  ", "Nội dung")));
    }

    @Test
    void danhSachRong_BaoLoi() {
        NhanVienService service = service(repoVoiCacNhanVien(), mock(EmailService.class));
        assertThrows(IllegalArgumentException.class,
            () -> service.guiEmailHangLoat(yeuCau(List.of())));
    }

    @Test
    void idTrungChiGuiMotLan() {
        NhanVien nv = nhanVien(1L, "Anhph", "Phạm Hà Anh", "nv1@example.com");
        EmailService emailService = mock(EmailService.class);
        when(emailService.guiMailCustom(anyString(), anyString(), anyString())).thenReturn(true);

        GuiEmailHangLoatResponse res = service(repoVoiCacNhanVien(nv), emailService)
            .guiEmailHangLoat(yeuCau(List.of(1L, 1L)));

        assertEquals(1, res.getTongYeuCau());
        assertEquals(1, res.getGuiThanhCong());
    }

    @Test
    void guiHangLoat_GuiSongSong_KhongQuaCham() throws Exception {
        int soNhanVien = 16;
        NhanVien[] ds = new NhanVien[soNhanVien];
        java.util.List<Long> ids = new java.util.ArrayList<>();
        for (int i = 0; i < soNhanVien; i++) {
            ds[i] = nhanVien((long) (i + 1), "NV" + i, "Nhân viên " + i, "nv" + i + "@example.com");
            ids.add((long) (i + 1));
        }
        EmailService emailService = mock(EmailService.class);
        when(emailService.guiMailCustom(anyString(), anyString(), anyString())).thenAnswer(inv -> {
            Thread.sleep(150);
            return true;
        });

        long start = System.nanoTime();
        GuiEmailHangLoatResponse res = service(repoVoiCacNhanVien(ds), emailService)
            .guiEmailHangLoat(yeuCau(ids));
        long elapsedMs = (System.nanoTime() - start) / 1_000_000;

        assertEquals(16, res.getTongYeuCau());
        assertEquals(16, res.getGuiThanhCong());
        // Tuần tiến sẽ mất 16 x 150ms = 2400ms. Gửi song song phải gọn trong ~600ms.
        assertTrue(elapsedMs < 1500,
            "Email phải được gửi song song, không được tuần tiến (mất " + elapsedMs + "ms)");
    }
}
