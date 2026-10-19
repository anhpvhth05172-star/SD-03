package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.NhanVien.NhanVienExcelImportResult;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;

class NhanVienExcelImportTest {

    private static final String[] COT_MAU = {
        "Họ và tên", "Email", "Số điện thoại", "Giới tính", "Ngày sinh",
        "Vai trò", "Địa chỉ", "Phường/Xã", "Quê quán", "Trạng thái"
    };

    private NhanVienRepository repo;
    private EmailService emailService;

    private NhanVienService service() {
        repo = mock(NhanVienRepository.class);
        when(repo.saveAndFlush(any(NhanVien.class))).thenAnswer(i -> i.getArgument(0));
        when(repo.save(any(NhanVien.class))).thenAnswer(i -> i.getArgument(0));

        VaiTroRepository vaiTroRepo = mock(VaiTroRepository.class);
        VaiTro staff = new VaiTro();
        staff.setId(3L);
        staff.setMaVaiTro("STAFF");
        staff.setTenVaiTro("Nhân viên");
        when(vaiTroRepo.findById(any())).thenReturn(Optional.of(staff));
        when(vaiTroRepo.findByMaVaiTroIn(any())).thenReturn(List.of(staff));

        emailService = mock(EmailService.class);
        return new NhanVienService(repo, vaiTroRepo, mock(PlatformTransactionManager.class), emailService);
    }

    @Test
    void taiMauExcelTraVeFileXlsxCoDayDuCot() throws Exception {
        byte[] bytes = service().downloadExcelTemplate();

        assertNotNull(bytes);
        assertTrue(bytes.length > 0, "File mẫu không được rỗng");
        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Row header = wb.getSheetAt(0).getRow(0);
            assertNotNull(header, "Dòng tiêu đề không được trống");
            DataFormatter fmt = new DataFormatter();
            List<String> cot = new ArrayList<>();
            for (int i = 0; i < header.getLastCellNum(); i++) {
                cot.add(fmt.formatCellValue(header.getCell(i)).trim());
            }
            for (String can : COT_MAU) {
                assertTrue(cot.contains(can), "Thiếu cột: " + can);
            }
        }
    }

    @Test
    void importExcelTaoNhanVienBaoKetQuaTheoDongVaKhongGuiMail() throws Exception {
        NhanVienService svc = service();
        byte[] file = fileExcel(
            dong("Nguyễn Văn An", "an@gmail.com", "0901111111", "Nam", "15/01/1990", "STAFF",
                "242 Xa Dan", "Văn Miếu", "Ha Noi", "Đang hoạt động"),
            dong("Thiếu email", "", "0902222222", "Nữ", "20/02/1995", "STAFF", "", "", "", ""),
            dong("Trùng email trong file", "an@gmail.com", "0903333333", "Nam", "21/03/1992", "STAFF", "", "", "", "")
        );

        NhanVienExcelImportResult res = svc.importFromExcel(
            new MockMultipartFile("file", "nv.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file));

        assertEquals(3, res.getTongSoDong());
        assertEquals(1, res.getSoDongThanhCong());
        assertEquals(2, res.getSoDongThatBai());
        assertEquals(2, res.getDanhSachLoi().size());
        for (String loi : res.getDanhSachLoi()) {
            assertTrue(loi.contains("Dòng "), "Lỗi phải ghi rõ số dòng: " + loi);
        }
        verify(repo, times(1)).saveAndFlush(any(NhanVien.class));
        verify(emailService, never()).guiMailNhanVienMoi(any(), any(), any(), any(), any(), any());
    }

    @Test
    void importExcelBaoLoiKhiEmailDaTonTaiTrongHeThong() throws Exception {
        NhanVienService svc = service();
        when(repo.existsByEmail("cu@x.com")).thenReturn(true);
        byte[] file = fileExcel(
            dong("Người cũ", "cu@x.com", "0904444444", "Nam", "10/10/1990", "STAFF", "", "", "", "")
        );

        NhanVienExcelImportResult res = svc.importFromExcel(
            new MockMultipartFile("file", "nv.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", file));

        assertEquals(1, res.getTongSoDong());
        assertEquals(0, res.getSoDongThanhCong());
        assertEquals(1, res.getSoDongThatBai());
        assertTrue(res.getDanhSachLoi().get(0).toLowerCase().contains("email"));
        verify(repo, never()).saveAndFlush(any(NhanVien.class));
    }

    private static String[] dong(String ten, String email, String sdt, String gioiTinh,
        String ngaySinh, String vaiTro, String diaChi, String phuong, String queQuan, String trangThai) {
        return new String[] {ten, email, sdt, gioiTinh, ngaySinh, vaiTro, diaChi, phuong, queQuan, trangThai};
    }

    private static byte[] fileExcel(String[]... cacDong) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("NhanVien");
            Row header = sheet.createRow(0);
            for (int i = 0; i < COT_MAU.length; i++) {
                header.createCell(i).setCellValue(COT_MAU[i]);
            }
            int soDong = 1;
            for (String[] dong : cacDong) {
                Row row = sheet.createRow(soDong++);
                for (int i = 0; i < dong.length; i++) {
                    row.createCell(i).setCellValue(dong[i]);
                }
            }
            java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
            wb.write(out);
            return out.toByteArray();
        }
    }
}
