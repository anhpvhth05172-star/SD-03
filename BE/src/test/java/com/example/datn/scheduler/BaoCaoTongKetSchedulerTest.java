package com.example.datn.scheduler;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class BaoCaoTongKetSchedulerTest {

    @Test
    void quyetDinhGui_ngayCuoiThangGuiThang() {
        assertEquals(List.of("THANG"), BaoCaoTongKetScheduler.quyetDinhGui(LocalDate.of(2026, 1, 31)));
        assertEquals(List.of("THANG"), BaoCaoTongKetScheduler.quyetDinhGui(LocalDate.of(2026, 4, 30)));
        assertEquals(List.of("THANG"), BaoCaoTongKetScheduler.quyetDinhGui(LocalDate.of(2026, 2, 28)));
    }

    @Test
    void quyetDinhGui_dauGiuaThangKhongGui() {
        assertTrue(BaoCaoTongKetScheduler.quyetDinhGui(LocalDate.of(2026, 10, 15)).isEmpty());
        assertTrue(BaoCaoTongKetScheduler.quyetDinhGui(LocalDate.of(2026, 10, 1)).isEmpty());
    }

    @Test
    void quyetDinhGui_cuoiNamGuiCaThangVaNam() {
        assertEquals(List.of("THANG", "NAM"), BaoCaoTongKetScheduler.quyetDinhGui(LocalDate.of(2026, 12, 31)));
    }

    @Test
    void quyetDinhGui_thangNhuanDungNgay29() {
        assertEquals(List.of("THANG"), BaoCaoTongKetScheduler.quyetDinhGui(LocalDate.of(2028, 2, 29)));
    }
}
