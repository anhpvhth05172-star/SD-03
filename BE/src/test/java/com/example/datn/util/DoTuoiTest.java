package com.example.datn.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class DoTuoiTest {

    private static final LocalDate HOM_NAY = LocalDate.of(2026, 10, 9);

    @Test
    void tinhTuoiTheoNgayThangNamThucTe() {
        assertEquals(16, DoTuoi.tinhTuoi(LocalDate.of(2010, 10, 9), HOM_NAY));
        assertEquals(15, DoTuoi.tinhTuoi(LocalDate.of(2010, 10, 10), HOM_NAY));
        assertEquals(16, DoTuoi.tinhTuoi(LocalDate.of(2010, 1, 1), HOM_NAY));
        assertEquals(17, DoTuoi.tinhTuoi(LocalDate.of(2009, 10, 9), HOM_NAY));
        assertEquals(16, DoTuoi.tinhTuoi(LocalDate.of(2009, 12, 31), HOM_NAY));
        assertEquals(-1, DoTuoi.tinhTuoi(null, HOM_NAY));
    }

    @Test
    void khongDungPhepHieuNamDonGian() {
        // Cùng hiệu năm nhưng chưa tới sinh nhật -> chưa đủ tuổi.
        assertEquals(15, DoTuoi.tinhTuoi(LocalDate.of(2010, 11, 1), HOM_NAY));
        assertFalse(DoTuoi.duTuoi(LocalDate.of(2010, 11, 1), 16, HOM_NAY));
    }

    @Test
    void dung16TuoiVuaDu() {
        assertTrue(DoTuoi.duTuoi(LocalDate.of(2010, 10, 9), 16, HOM_NAY));
        assertFalse(DoTuoi.duTuoi(LocalDate.of(2010, 10, 10), 16, HOM_NAY));
        assertFalse(DoTuoi.duTuoi(LocalDate.of(2011, 1, 1), 16, HOM_NAY));
    }

    @Test
    void dung18TuoiVuaDu() {
        assertTrue(DoTuoi.duTuoi(LocalDate.of(2008, 10, 9), 18, HOM_NAY));
        assertFalse(DoTuoi.duTuoi(LocalDate.of(2008, 10, 10), 18, HOM_NAY));
        assertFalse(DoTuoi.duTuoi(LocalDate.of(2009, 1, 1), 18, HOM_NAY));
    }

    @Test
    void kiemTraTraVeThongBao() {
        LocalDate nay = LocalDate.now();
        assertNull(DoTuoi.kiemTra(null, 16, "phai du 16"));
        assertNull(DoTuoi.kiemTra(nay.minusYears(16), 16, "phai du 16"));
        assertEquals("phai du 16", DoTuoi.kiemTra(nay.minusYears(16).plusDays(1), 16, "phai du 16"));
        assertEquals("phai du 16", DoTuoi.kiemTra(nay.minusYears(15), 16, "phai du 16"));
        assertEquals("Ngày sinh không được ở tương lai",
            DoTuoi.kiemTra(nay.plusDays(1), 16, "phai du 16"));
        assertNull(DoTuoi.kiemTra(nay.minusYears(18), 18, "phai du 18"));
        assertEquals("phai du 18", DoTuoi.kiemTra(nay.minusYears(18).plusDays(1), 18, "phai du 18"));
    }
}
