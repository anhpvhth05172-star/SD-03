package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.LichLamViec.ImportLichRequest;
import com.example.datn.dto.LichLamViec.ImportLichResponse;
import com.example.datn.dto.LichLamViec.ImportLichRow;
import com.example.datn.dto.LichLamViec.LichLamViecDTO;
import com.example.datn.dto.LichLamViec.LichLamViecRequest;
import com.example.datn.dto.LichLamViec.XepTuDongResponse;
import com.example.datn.entity.CaLam;
import com.example.datn.entity.LichLamViec;
import com.example.datn.entity.NhanVien;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.CaLamRepository;
import com.example.datn.repository.LichLamViecRepository;
import com.example.datn.repository.NhanVienRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class LichLamViecServiceTest {

    private final LichLamViecRepository lichRepo = mock(LichLamViecRepository.class);
    private final NhanVienRepository nvRepo = mock(NhanVienRepository.class);
    private final CaLamRepository caRepo = mock(CaLamRepository.class);

    private LichLamViecService service() {
        return new LichLamViecService(lichRepo, nvRepo, caRepo);
    }

    private static NhanVien nv(long id, String ma) {
        NhanVien n = new NhanVien();
        n.setId(id);
        n.setMaNhanVien(ma);
        n.setTenTaiKhoan("nv" + id);
        n.setTrangThai(true);
        return n;
    }

    private static CaLam ca(long id, String ma, String ten, String batDau, String ketThuc, boolean dangBat) {
        CaLam c = new CaLam();
        c.setId(id);
        c.setMaCa(ma);
        c.setTenCa(ten);
        c.setBatDau(LocalTime.parse(batDau));
        c.setKetThuc(LocalTime.parse(ketThuc));
        c.setDangBat(dangBat);
        return c;
    }

    private static LichLamViec lich(long id, NhanVien n, CaLam c, LocalDate ngay) {
        LichLamViec l = new LichLamViec();
        l.setId(id);
        l.setNhanVien(n);
        l.setCaLam(c);
        l.setNgayLam(ngay);
        return l;
    }

    private void mockCreateFixtures() {
        when(nvRepo.findByMaNhanVien("NV001")).thenReturn(Optional.of(nv(1, "NV001")));
        when(caRepo.findById(1L)).thenReturn(Optional.of(ca(1, "sang", "Ca sáng", "08:00", "12:00", true)));
        when(lichRepo.existsByNhanVienIdAndNgayLamAndCaLamId(1L, LocalDate.of(2026, 10, 20), 1L)).thenReturn(false);
        when(lichRepo.save(any())).thenAnswer(inv -> {
            LichLamViec l = inv.getArgument(0);
            l.setId(10L);
            return l;
        });
    }

    @Test
    void create_hopLeTraDTO() {
        mockCreateFixtures();

        LichLamViecDTO dto = service().create(new LichLamViecRequest("NV001", 1L, LocalDate.of(2026, 10, 20)));

        assertEquals(10L, dto.id());
        assertEquals("NV001", dto.maNhanVien());
        assertEquals("Ca sáng", dto.tenCa());
        assertEquals(LocalTime.of(8, 0), dto.batDau());
        assertEquals(LocalDate.of(2026, 10, 20), dto.ngay());
    }

    @Test
    void create_trungLichBi400() {
        when(nvRepo.findByMaNhanVien("NV001")).thenReturn(Optional.of(nv(1, "NV001")));
        when(caRepo.findById(1L)).thenReturn(Optional.of(ca(1, "sang", "Ca sáng", "08:00", "12:00", true)));
        when(lichRepo.existsByNhanVienIdAndNgayLamAndCaLamId(1L, LocalDate.of(2026, 10, 20), 1L)).thenReturn(true);

        DuplicateRecordException ex = assertThrows(DuplicateRecordException.class,
            () -> service().create(new LichLamViecRequest("NV001", 1L, LocalDate.of(2026, 10, 20))));
        assertTrue(ex.getMessage().contains("Trống lịch làm việc") || ex.getMessage().contains("trống"));
    }

    @Test
    void update_boQuaId() {
        NhanVien n = nv(1, "NV001");
        CaLam c = ca(1, "sang", "Ca sáng", "08:00", "12:00", true);
        LocalDate ngay = LocalDate.of(2026, 10, 20);
        LichLamViec existing = lich(2, n, c, ngay);
        when(lichRepo.findById(2L)).thenReturn(Optional.of(existing));
        when(nvRepo.findByMaNhanVien("NV001")).thenReturn(Optional.of(n));
        when(caRepo.findById(1L)).thenReturn(Optional.of(c));
        when(lichRepo.existsByNhanVienIdAndNgayLamAndCaLamIdAndIdNot(1L, ngay, 1L, 2L)).thenReturn(false);
        when(lichRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        LichLamViecDTO dto = service().update(2L, new LichLamViecRequest("NV001", 1L, ngay));
        assertEquals(2L, dto.id());
    }

    @Test
    void create_caTatBi400() {
        when(nvRepo.findByMaNhanVien("NV001")).thenReturn(Optional.of(nv(1, "NV001")));
        when(caRepo.findById(1L)).thenReturn(Optional.of(ca(1, "sang", "Ca sáng", "08:00", "12:00", false)));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service().create(new LichLamViecRequest("NV001", 1L, LocalDate.of(2026, 10, 20))));
        assertTrue(ex.getMessage().contains("Ca sáng"));
    }

    @Test
    void xepTuDong_TuDongPhanCa() {
        when(caRepo.findByDangBatTrueOrderByBatDauAsc()).thenReturn(List.of(
            ca(1, "sang", "Ca sáng", "08:00", "12:00", true),
            ca(2, "chieu", "Ca chiều", "13:00", "17:00", true)
        ));
        when(nvRepo.findByTrangThaiTrue()).thenReturn(List.of(
            nv(1, "NV001"), nv(2, "NV002"), nv(3, "NV003"), nv(4, "NV004"), nv(5, "NV005")
        ));
        LocalDate ngay = LocalDate.of(2026, 10, 20);
        when(lichRepo.existsByNhanVienIdAndNgayLamAndCaLamId(any(), any(), any())).thenReturn(false);
        when(lichRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        XepTuDongResponse resp = service().xepTuDong(ngay);

        assertEquals(null, resp.lyDo());
        assertEquals(5, resp.themMoi());
        verify(lichRepo).saveAll(anyList());
    }

    @Test
    void xepTuDong_KhongCa() {
        when(caRepo.findByDangBatTrueOrderByBatDauAsc()).thenReturn(List.of());

        XepTuDongResponse resp = service().xepTuDong(LocalDate.of(2026, 10, 20));
        assertEquals("khongCa", resp.lyDo());
        assertEquals(0, resp.themMoi());
        verify(lichRepo, never()).saveAll(anyList());
    }

    @Test
    void xepTuDong_KhongUngVien() {
        when(caRepo.findByDangBatTrueOrderByBatDauAsc()).thenReturn(List.of(
            ca(1, "sang", "Ca sáng", "08:00", "12:00", true)
        ));
        when(nvRepo.findByTrangThaiTrue()).thenReturn(List.of());

        XepTuDongResponse resp = service().xepTuDong(LocalDate.of(2026, 10, 20));
        assertEquals("khongUngVien", resp.lyDo());
        assertEquals(0, resp.themMoi());
    }

    @Test
    void import_batTrungTrongBatch() {
        LocalDate ngay = LocalDate.of(2026, 10, 20);
        when(nvRepo.findByMaNhanVien("NV001")).thenReturn(Optional.of(nv(1, "NV001")));
        when(caRepo.findByMaCa("sang")).thenReturn(Optional.of(ca(1, "sang", "Ca sáng", "08:00", "12:00", true)));
        when(lichRepo.existsByNhanVienIdAndNgayLamAndCaLamId(1L, ngay, 1L)).thenReturn(false);
        when(lichRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportLichRequest req = new ImportLichRequest(List.of(
            new ImportLichRow("NV001", "sang", ngay),
            new ImportLichRow("NV001", "sang", ngay)
        ));
        ImportLichResponse resp = service().importLich(req);

        assertEquals(1, resp.themMoi());
        assertEquals(1, resp.loi().size());
        assertEquals(2, resp.loi().get(0).dong());
        assertTrue(resp.loi().get(0).lyDo().contains("Trống"));
    }

    @Test
    void import_batCaTatVaKhongTonTai() {
        LocalDate ngay = LocalDate.of(2026, 10, 20);
        when(nvRepo.findByMaNhanVien("NV001")).thenReturn(Optional.of(nv(1, "NV001")));
        when(nvRepo.findByMaNhanVien("NV999")).thenReturn(Optional.empty());
        when(caRepo.findByMaCa("sang")).thenReturn(Optional.of(ca(1, "sang", "Ca sáng", "08:00", "12:00", false)));
        when(caRepo.findByMaCa("chieu")).thenReturn(Optional.of(ca(2, "chieu", "Ca chiều", "13:00", "17:00", true)));
        when(lichRepo.existsByNhanVienIdAndNgayLamAndCaLamId(1L, ngay, 2L)).thenReturn(false);
        when(lichRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ImportLichRequest req = new ImportLichRequest(List.of(
            new ImportLichRow("NV001", "sang", ngay),
            new ImportLichRow("NV999", "chieu", ngay),
            new ImportLichRow("NV001", "chieu", ngay)
        ));
        ImportLichResponse resp = service().importLich(req);

        assertEquals(1, resp.themMoi());
        assertEquals(2, resp.loi().size());
        assertEquals(1, resp.loi().get(0).dong());
        assertEquals(2, resp.loi().get(1).dong());
        assertTrue(resp.loi().get(0).lyDo().contains("Ca sáng"));
        assertTrue(resp.loi().get(1).lyDo().contains("NV999"));
    }

    @Test
    void deleteNgay_xoaHet() {
        LocalDate ngay = LocalDate.of(2026, 10, 20);
        service().deleteTheoNgay(ngay);
        verify(lichRepo).deleteByNgayLam(ngay);
    }
}
