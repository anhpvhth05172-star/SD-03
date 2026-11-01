package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.LichLamViec.CaLamDTO;
import com.example.datn.dto.LichLamViec.CaLamRequest;
import com.example.datn.entity.CaLam;
import com.example.datn.exception.BiChanException;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.repository.CaLamRepository;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class CaLamServiceTest {

    private final CaLamRepository repo = mock(CaLamRepository.class);

    private CaLamService service() {
        return new CaLamService(repo);
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

    @Test
    void create_sinhMaVaTraDTO() {
        when(repo.existsByTenCaIgnoreCase(any())).thenReturn(false);
        when(repo.existsByMaCa(any())).thenReturn(false);
        when(repo.save(any())).thenAnswer(inv -> {
            CaLam c = inv.getArgument(0);
            c.setId(10L);
            return c;
        });

        CaLamDTO dto = service().create(new CaLamRequest("Ca sáng mới", LocalTime.of(8, 0), LocalTime.of(12, 0)));

        assertEquals("ca-sang-moi", dto.ma());
        assertEquals("Ca sáng mới", dto.ten());
        assertTrue(dto.dangBat());
        assertEquals(LocalTime.of(8, 0), dto.batDau());
        assertEquals(LocalTime.of(12, 0), dto.ketThuc());
    }

    @Test
    void create_trungTenBi400() {
        when(repo.existsByTenCaIgnoreCase("Ca sáng")).thenReturn(true);

        DuplicateRecordException ex = assertThrows(DuplicateRecordException.class,
            () -> service().create(new CaLamRequest("Ca sáng", LocalTime.of(8, 0), LocalTime.of(12, 0))));
        assertTrue(ex.getMessage().contains("tồn tại"));
    }

    @Test
    void create_ketThucTruocBatDauBi400() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service().create(new CaLamRequest("Ca test", LocalTime.of(12, 0), LocalTime.of(8, 0))));
        assertTrue(ex.getMessage().contains("không hợp lệ"));
    }

    @Test
    void create_trungKhungGioBi400() {
        when(repo.existsByTenCaIgnoreCase("Ca trung gian")).thenReturn(false);
        when(repo.existsByMaCa(any())).thenReturn(false);
        when(repo.findAll()).thenReturn(List.of(
            ca(1, "sang", "Ca sáng", "08:00", "12:00", true)
        ));

        DuplicateRecordException ex = assertThrows(DuplicateRecordException.class,
            () -> service().create(new CaLamRequest("Ca trung gian", LocalTime.of(9, 0), LocalTime.of(13, 0))));
        assertTrue(ex.getMessage().contains("Ca sáng"));
    }

    @Test
    void create_khongTrungKhungGio_OK() {
        when(repo.existsByTenCaIgnoreCase(any())).thenReturn(false);
        when(repo.existsByMaCa(any())).thenReturn(false);
        when(repo.findAll()).thenReturn(List.of(
            ca(1, "sang", "Ca sáng", "08:00", "12:00", true)
        ));
        when(repo.save(any())).thenAnswer(inv -> {
            CaLam c = inv.getArgument(0);
            c.setId(2L);
            return c;
        });

        CaLamDTO dto = service().create(new CaLamRequest("Ca chiều mới", LocalTime.of(12, 0), LocalTime.of(14, 0)));
        assertEquals("ca-chieu-moi", dto.ma());
    }

    @Test
    void update_boQuaIdHienTai() {
        CaLam existing = ca(1, "sang", "Ca sáng", "08:00", "12:00", true);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.existsByTenCaIgnoreCaseAndIdNot("Ca sáng mới", 1L)).thenReturn(false);
        when(repo.findAll()).thenReturn(List.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CaLamDTO dto = service().update(1L, new CaLamRequest("Ca sáng mới", LocalTime.of(8, 0), LocalTime.of(12, 0)));
        assertEquals("Ca sáng mới", dto.ten());
        assertEquals(1L, dto.id());
    }

    @Test
    void xoaCaConLichGanBiChan() {
        when(repo.findById(1L)).thenReturn(Optional.of(ca(1, "sang", "Ca sáng", "08:00", "12:00", true)));
        when(repo.demLichTheoCa(1L)).thenReturn(2L);

        BiChanException ex = assertThrows(BiChanException.class, () -> service().delete(1L));
        assertTrue(ex.getMessage().contains("2"));
        assertTrue(ex.getMessage().contains("Ca sáng"));
    }

    @Test
    void xoaCaKhongLich_OK() {
        when(repo.findById(1L)).thenReturn(Optional.of(ca(1, "sang", "Ca sáng", "08:00", "12:00", true)));
        when(repo.demLichTheoCa(1L)).thenReturn(0L);

        service().delete(1L);
        verify(repo).deleteById(1L);
    }

    @Test
    void toggle_doiTrangThai() {
        CaLam existing = ca(1, "sang", "Ca sáng", "08:00", "12:00", true);
        when(repo.findById(1L)).thenReturn(Optional.of(existing));
        when(repo.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CaLamDTO dto = service().toggle(1L);
        assertFalse(dto.dangBat());
    }

    @Test
    void list_sapXepTheoBatDau() {
        when(repo.findAll()).thenReturn(List.of(
            ca(3, "toi", "Ca tối", "17:30", "21:30", true),
            ca(1, "sang", "Ca sáng", "08:00", "12:00", true),
            ca(2, "chieu", "Ca chiều", "13:00", "17:00", true)
        ));

        List<CaLamDTO> list = service().list();
        assertEquals(3, list.size());
        assertEquals("sang", list.get(0).ma());
        assertEquals("chieu", list.get(1).ma());
        assertEquals("toi", list.get(2).ma());
    }
}
