package com.example.datn.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaNhanVienTest {

    @Test
    void sinhTheoQuyTacTrongYeuCau() {
        assertEquals("Binhnt", MaNhanVien.sinh("Nguyễn Thị Bình"));
        assertEquals("Binhnv", MaNhanVien.sinh("Nguyễn Văn Bình"));
        assertEquals("Anhtv", MaNhanVien.sinh("Trần Văn Anh"));
        assertEquals("Anhph", MaNhanVien.sinh("Phạm Hà Anh"));
    }

    @Test
    void boDauVaChuanHoaKhoangTrang() {
        assertEquals("Binhnt", MaNhanVien.sinh("  Nguyễn   Thị   Bình "));
        assertEquals("Anhph", MaNhanVien.sinh("  Phạm   Hà   Anh "));
        assertEquals("Anlh", MaNhanVien.sinh("Lý Hoàng An"));
        assertEquals("Namvv", MaNhanVien.sinh("Võ Văn Nam"));
        assertEquals("Annv", MaNhanVien.sinh("Nguyễn Văn An"));
    }

    @Test
    void tenMotTuVaTenRong() {
        assertEquals("Anh", MaNhanVien.sinh("Anh"));
        assertEquals("NV", MaNhanVien.sinh("  "));
        assertEquals("NV", MaNhanVien.sinh(null));
    }

    @Test
    void congSoHaiChuSoTu01() {
        assertEquals("Binhnt01", MaNhanVien.congSo("Binhnt", 1));
        assertEquals("Binhnt02", MaNhanVien.congSo("Binhnt", 2));
        assertEquals("Binhnt09", MaNhanVien.congSo("Binhnt", 9));
        assertEquals("Binhnt99", MaNhanVien.congSo("Binhnt", 99));
    }

    @Test
    void congSoVuot99GiuyenSoChuSo() {
        assertEquals("Binhnt100", MaNhanVien.congSo("Binhnt", 100));
        assertEquals("Binhnt101", MaNhanVien.congSo("Binhnt", 101));
        assertEquals("Binhnt1000", MaNhanVien.congSo("Binhnt", 1000));
    }

    @Test
    void congSoKhongVuotDoiDaiToiDa() {
        String goc = MaNhanVien.sinh("Nguyễn Văn " + "x".repeat(40));
        String ma = MaNhanVien.congSo(goc, 1);
        assertEquals(MaNhanVien.TOI_DA, ma.length());
        assertTrue(MaNhanVien.hopLe(ma));

        String ma100 = MaNhanVien.congSo(goc, 100);
        assertEquals(MaNhanVien.TOI_DA, ma100.length());
        assertTrue(MaNhanVien.hopLe(ma100));
    }

    @Test
    void congSoMacDinhKhiGocRong() {
        assertEquals("NV01", MaNhanVien.congSo(null, 1));
        assertEquals("NV01", MaNhanVien.congSo("  ", 1));
        assertEquals("NV100", MaNhanVien.congSo("", 100));
    }

    @Test
    void maHopLe() {
        assertTrue(MaNhanVien.hopLe("Binhnt01"));
        assertTrue(MaNhanVien.hopLe("Anhph"));
        assertTrue(MaNhanVien.hopLe("NV001"));
        assertTrue(MaNhanVien.hopLe("Anhph2"));
        assertFalse(MaNhanVien.hopLe("nv01!"));
        assertFalse(MaNhanVien.hopLe("1abc"));
        assertFalse(MaNhanVien.hopLe(""));
        assertFalse(MaNhanVien.hopLe(null));
        assertFalse(MaNhanVien.hopLe("a".repeat(26)));
    }

    @Test
    void gioiHanDai() {
        assertEquals(25, MaNhanVien.sinh("Nguyễn Văn " + "x".repeat(40)).length());
        assertTrue(MaNhanVien.hopLe(MaNhanVien.sinh("Nguyễn Văn " + "x".repeat(40))));
    }
}
