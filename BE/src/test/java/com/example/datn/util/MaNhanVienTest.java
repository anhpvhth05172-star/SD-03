package com.example.datn.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MaNhanVienTest {

    @Test
    void sinhTheoQuyTacTrongYeuCau() {
        assertEquals("BinhNT", MaNhanVien.sinh("Nguyễn Thị Bình"));
        assertEquals("BinhNV", MaNhanVien.sinh("Nguyễn Văn Bình"));
        assertEquals("AnhTV", MaNhanVien.sinh("Trần Văn Anh"));
        assertEquals("AnhPVH", MaNhanVien.sinh("Phạm Vũ Hoàng Anh"));
        assertEquals("AnhPH", MaNhanVien.sinh("Phạm Hà Anh"));
    }

    @Test
    void boDauVaChuanHoaKhoangTrang() {
        assertEquals("BinhNT", MaNhanVien.sinh("  Nguyễn   Thị   Bình "));
        assertEquals("AnhPVH", MaNhanVien.sinh("  Phạm   Vũ   Hoàng   Anh "));
        assertEquals("AnLH", MaNhanVien.sinh("Lý Hoàng An"));
        assertEquals("NamVV", MaNhanVien.sinh("Võ Văn Nam"));
        assertEquals("AnNV", MaNhanVien.sinh("Nguyễn Văn An"));
    }

    @Test
    void tenMotTuVaTenRong() {
        assertEquals("Anh", MaNhanVien.sinh("Anh"));
        assertEquals("NV", MaNhanVien.sinh("  "));
        assertEquals("NV", MaNhanVien.sinh(null));
    }

    @Test
    void congSoTrucTiepTu2() {
        assertEquals("AnhPVH", MaNhanVien.congSo("AnhPVH", 1));
        assertEquals("AnhPVH2", MaNhanVien.congSo("AnhPVH", 2));
        assertEquals("AnhPVH3", MaNhanVien.congSo("AnhPVH", 3));
        assertEquals("AnhPVH99", MaNhanVien.congSo("AnhPVH", 99));
    }

    @Test
    void congSoVuot99GiuyenSoChuSo() {
        assertEquals("BinhNT100", MaNhanVien.congSo("BinhNT", 100));
        assertEquals("BinhNT101", MaNhanVien.congSo("BinhNT", 101));
        assertEquals("BinhNT1000", MaNhanVien.congSo("BinhNT", 1000));
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
        assertEquals("NV", MaNhanVien.congSo(null, 1));
        assertEquals("NV", MaNhanVien.congSo("  ", 1));
        assertEquals("NV2", MaNhanVien.congSo(null, 2));
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

    /** Mã sinh ra phải LUÔN hợp lệ (bắt đầu bằng chữ cái) để import lại cập nhật được. */
    @Test
    void sinhTenKetThucBangSo_VanBatDauBangChuCai() {
        assertEquals("NV1234567EO", MaNhanVien.sinh("E2E One 1234567"));
        assertEquals("NV1234567", MaNhanVien.sinh("1234567"));
        assertEquals("NV123", MaNhanVien.sinh("123"));
        assertTrue(MaNhanVien.hopLe(MaNhanVien.sinh("E2E One 1234567")));
        assertTrue(MaNhanVien.hopLe(MaNhanVien.sinh("1234567")));
        assertTrue(MaNhanVien.hopLe(MaNhanVien.congSo(MaNhanVien.sinh("E2E One 1234567"), 2)));
    }

    @Test
    void gioiHanDai() {
        assertEquals(25, MaNhanVien.sinh("Nguyễn Văn " + "x".repeat(40)).length());
        assertTrue(MaNhanVien.hopLe(MaNhanVien.sinh("Nguyễn Văn " + "x".repeat(40))));
    }
}
