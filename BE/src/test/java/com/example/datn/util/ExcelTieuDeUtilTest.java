package com.example.datn.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

class ExcelTieuDeUtilTest {

    private static final String[] MAU = {
        "Mã nhân viên (trống = tự sinh)", "Họ và tên *", "Email"
    };
    private static final String[] KY_MON = {"ma nhan vien", "ho va ten", "email"};

    @Test
    void chuanHoa_boDichVietHoaThuong() {
        assertEquals("ho va ten", ExcelTieuDeUtil.chuanHoa("  HỌ VÀ Tên  "));
        assertEquals("ma nhan vien", ExcelTieuDeUtil.chuanHoa("Mã nhân viên"));
        assertEquals("dia chi cu the", ExcelTieuDeUtil.chuanHoa("Địa chỉ cụ thể"));
        assertEquals("", ExcelTieuDeUtil.chuanHoa(null));
    }

    @Test
    void kiemTraTieuDe_tieuDeDung_KhongBaoLoi() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("S");
            Row row = sheet.createRow(0);
            for (int i = 0; i < MAU.length; i++) {
                row.createCell(i).setCellValue(MAU[i]);
            }
            assertDoesNotThrow(() -> ExcelTieuDeUtil.kiemTraTieuDe(
                sheet, new DataFormatter(), KY_MON, MAU));
        }
    }

    @Test
    void kiemTraTieuDe_thieuHangTieuDe_BaoLoi() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("S");
            sheet.createRow(1).createCell(0).setCellValue("dữ liệu");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ExcelTieuDeUtil.kiemTraTieuDe(sheet, new DataFormatter(), KY_MON, MAU));
            assertTrue(ex.getMessage().contains("thiếu hàng tiêu đề"));
        }
    }

    @Test
    void kiemTraTieuDe_saiThuTu_BaoLoiChiSoVaMongDoi() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("S");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Họ và tên *");
            row.createCell(1).setCellValue("Mã nhân viên (trống = tự sinh)");
            row.createCell(2).setCellValue("Email");
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ExcelTieuDeUtil.kiemTraTieuDe(sheet, new DataFormatter(), KY_MON, MAU));
            assertTrue(ex.getMessage().contains("cột thứ 1"));
            assertTrue(ex.getMessage().contains("Họ và tên *"));
            assertTrue(ex.getMessage().contains("Mã nhân viên"));
        }
    }

    @Test
    void kiemTraTieuDe_thieuCot_BaoLoi() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("S");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue(MAU[0]);
            row.createCell(1).setCellValue(MAU[1]);
            // cột thứ 3 trống
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> ExcelTieuDeUtil.kiemTraTieuDe(sheet, new DataFormatter(), KY_MON, MAU));
            assertTrue(ex.getMessage().contains("thiếu cột bắt buộc thứ 3"));
        }
    }

    @Test
    void kiemTraTieuDe_kyTuDacBietVanKhopSauKhiChuanHoa() throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("S");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("Mã nhân viên (trống = tự sinh)");
            row.createCell(1).setCellValue("ho và Tên *");
            row.createCell(2).setCellValue("EMAIL:");
            assertDoesNotThrow(() -> ExcelTieuDeUtil.kiemTraTieuDe(
                sheet, new DataFormatter(), KY_MON, MAU));
        }
    }

    @Test
    void mauVaKyMon_CungDoDai() {
        assertEquals(MAU.length, KY_MON.length);
        assertEquals(Arrays.stream(MAU).count(), Arrays.stream(KY_MON).count());
    }
}
