package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.NhanVien.NhanVienImportKetQuaResponse;
import com.example.datn.dto.NhanVien.NhanVienImportPreviewResponse;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Optional;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;

class NhanVienImportServiceTest {

    /** Khớp chính xác hàng tiêu đề mà file xuất của frontend tạo ra. */
    private static final String[] TIEU_DE_MAU = {
        "Mã nhân viên (trống = tự sinh)", "Họ và tên *", "Email", "Số điện thoại",
        "Giới tính (Nam / Nữ / Khác)", "Ngày sinh (dd/MM/yyyy)", "Vai trò (ADMIN / STAFF)",
        "Tỉnh/Thành phố", "Xã/Phường/Thị trấn", "Địa chỉ cụ thể"
    };

    private static final String[] DONG_HOP_LE = {
        "", "Nguyễn Thị Bình", "binh@example.com", "0901234567", "Nam", "15/03/1990",
        "STAFF", "Hà Nội", "Cầu Giấy", "Số 1"
    };

    private static VaiTroRepository vaiTroRepository() {
        VaiTro staff = new VaiTro();
        staff.setId(1L);
        staff.setMaVaiTro("STAFF");
        staff.setTenVaiTro("Nhân viên");
        VaiTroRepository repo = mock(VaiTroRepository.class);
        when(repo.findAll()).thenReturn(List.of(staff));
        when(repo.findById(anyLong())).thenReturn(Optional.of(staff));
        return repo;
    }

    private static NhanVienRepository nhanVienRepository() {
        NhanVienRepository repo = mock(NhanVienRepository.class);
        when(repo.findByMaNhanVien(anyString())).thenReturn(Optional.empty());
        when(repo.findById(anyLong())).thenReturn(Optional.empty());
        return repo;
    }

    private static NhanVienService nhanVienService(NhanVienRepository nvRepo, VaiTroRepository vtRepo) {
        return new NhanVienService(nvRepo, vtRepo, mock(PlatformTransactionManager.class),
            mock(EmailService.class));
    }

    private static NhanVienImportService importService(
        NhanVienService nhanVienService,
        NhanVienRepository nvRepo,
        VaiTroRepository vtRepo
    ) {
        return new NhanVienImportService(nhanVienService, nvRepo, vtRepo);
    }

    private static MockMultipartFile fileExcel(String[]... cacDong) throws Exception {
        return fileExcelTieuDe(TIEU_DE_MAU, cacDong);
    }

    /** Tạo tệp Excel với hàng tiêu đề tùy chỉnh (dòng 0) + các dòng dữ liệu (dòng 1 trở đi). */
    private static MockMultipartFile fileExcelTieuDe(String[] tieuDe, String[]... cacDong) throws Exception {
        try (XSSFWorkbook wb = new XSSFWorkbook()) {
            Sheet sheet = wb.createSheet("Nhan vien");
            org.apache.poi.ss.usermodel.Row header = sheet.createRow(0);
            for (int i = 0; i < tieuDe.length; i++) {
                header.createCell(i).setCellValue(tieuDe[i]);
            }
            int r = 1;
            for (String[] values : cacDong) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r++);
                for (int i = 0; i < values.length; i++) {
                    row.createCell(i).setCellValue(values[i]);
                }
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            wb.write(out);
            return new MockMultipartFile(
                "file", "nhan-vien.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
            );
        }
    }

    @Test
    void taoFileMau_docLaiDuocTieuDeCacCot() throws Exception {
        NhanVienImportService service = importService(
            nhanVienService(nhanVienRepository(), vaiTroRepository()),
            nhanVienRepository(), vaiTroRepository());

        byte[] bytes = service.taoFileMau();
        assertNotNull(bytes);
        assertTrue(bytes.length > 0);

        try (Workbook wb = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            Sheet sheet = wb.getSheetAt(0);
            assertNotNull(sheet);
            assertTrue(sheet.getRow(0).getCell(1).getStringCellValue().contains("Họ và tên"));
            assertTrue(sheet.getRow(0).getCell(0).getStringCellValue().contains("Mã nhân viên"));
        }
    }

    @Test
    void xemTruoc_phanBietDongHopLeVaDongLoi() throws Exception {
        NhanVienRepository nvRepo = nhanVienRepository();
        NhanVienImportService service = importService(
            nhanVienService(nvRepo, vaiTroRepository()), nvRepo, vaiTroRepository());

        String[] dongLoiThieuTen = {
            "", "", "loisai@example.com", "0901234567", "Nam", "15/03/1990",
            "STAFF", "Hà Nội", "Cầu Giấy", "Số 2"
        };
        NhanVienImportPreviewResponse preview =
            service.xemTruoc(fileExcel(DONG_HOP_LE, dongLoiThieuTen));

        assertEquals(2, preview.getTongSoDong());
        assertEquals(1, preview.getDongHopLe());
        assertEquals(1, preview.getDongLoi());
        assertEquals(1, preview.getThemMoiDuKien());
        assertEquals(0, preview.getCapNhatDuKien());
        assertEquals("THEM", preview.getDong().get(0).getHanhDong());
        assertNull(preview.getDong().get(0).getLyDoLoi());
        assertNotNull(preview.getDong().get(1).getLyDoLoi());
        assertTrue(preview.getLoi().get(0).contains("Tên nhân viên"));
    }

    @Test
    void xemTruoc_maTrongFileBiLap_BaoLoiDongSau() throws Exception {
        NhanVienRepository nvRepo = nhanVienRepository();
        NhanVienImportService service = importService(
            nhanVienService(nvRepo, vaiTroRepository()), nvRepo, vaiTroRepository());

        String[] dong1 = DONG_HOP_LE.clone();
        dong1[0] = "NV09";
        String[] dong2 = DONG_HOP_LE.clone();
        dong2[0] = "nv09";
        dong2[2] = "khac@example.com";

        NhanVienImportPreviewResponse preview = service.xemTruoc(fileExcel(dong1, dong2));

        assertEquals(1, preview.getDongHopLe());
        assertEquals(1, preview.getDongLoi());
        assertTrue(preview.getDong().get(1).getLyDoLoi().contains("lặp lại"));
    }

    @Test
    void xemTruoc_maDaTonTaiTrenHeThong_ThongBaoCapNhat() throws Exception {
        NhanVien cu = new NhanVien();
        cu.setId(7L);
        cu.setMaNhanVien("NV01");
        cu.setTenTaiKhoan("Cũ Có");
        cu.setEmail("cucu@example.com");
        cu.setTrangThai(true);

        NhanVienRepository nvRepo = mock(NhanVienRepository.class);
        when(nvRepo.findByMaNhanVien("NV01")).thenReturn(Optional.of(cu));
        when(nvRepo.findById(7L)).thenReturn(Optional.of(cu));
        when(nvRepo.save(any(NhanVien.class))).thenAnswer(inv -> inv.getArgument(0));

        NhanVienImportService service = importService(
            nhanVienService(nvRepo, vaiTroRepository()), nvRepo, vaiTroRepository());

        String[] dong = DONG_HOP_LE.clone();
        dong[0] = "NV01";
        NhanVienImportPreviewResponse preview = service.xemTruoc(fileExcel(dong));

        assertEquals(1, preview.getCapNhatDuKien());
        assertEquals(0, preview.getThemMoiDuKien());
        assertEquals("CAP_NHAT", preview.getDong().get(0).getHanhDong());
        assertNull(preview.getDong().get(0).getLyDoLoi());
    }

    @Test
    void xacNhan_taoMoiVaCapNhatTungDong() throws Exception {
        NhanVien cu = new NhanVien();
        cu.setId(7L);
        cu.setMaNhanVien("NV01");
        cu.setTrangThai(true);

        NhanVienService nhanVienService = mock(NhanVienService.class);
        NhanVienRepository nvRepo = mock(NhanVienRepository.class);
        when(nvRepo.findByMaNhanVien("NV01")).thenReturn(Optional.of(cu));
        when(nvRepo.findById(7L)).thenReturn(Optional.of(cu));

        NhanVienImportService service = importService(nhanVienService, nvRepo, vaiTroRepository());

        String[] dongMoi = DONG_HOP_LE.clone();
        String[] dongCapNhat = DONG_HOP_LE.clone();
        dongCapNhat[0] = "NV01";
        dongCapNhat[2] = "aicapnhat@example.com";
        dongCapNhat[3] = "0901234568";

        NhanVienImportKetQuaResponse result =
            service.xacNhan(fileExcel(dongMoi, dongCapNhat));

        assertEquals(1, result.getThemThanhCong());
        assertEquals(1, result.getCapNhatThanhCong());
        assertEquals(0, result.getThatBai());
        verify(nhanVienService).create(any(), eq(false));
        verify(nhanVienService).update(eq(7L), any());
    }

    @Test
    void xacNhan_dongLoiKhongAnhHuongDongKhac() throws Exception {
        NhanVienService nhanVienService = mock(NhanVienService.class);
        when(nhanVienService.create(any(), eq(false)))
            .thenThrow(new IllegalArgumentException("Email đã được sử dụng bởi nhân viên khác"));
        NhanVienRepository nvRepo = mock(NhanVienRepository.class);
        when(nvRepo.findByMaNhanVien(anyString())).thenReturn(Optional.empty());

        NhanVienImportService service = importService(nhanVienService, nvRepo, vaiTroRepository());

        String[] dong1 = DONG_HOP_LE.clone();
        String[] dong2 = DONG_HOP_LE.clone();
        dong2[2] = "trung@example.com";

        NhanVienImportKetQuaResponse result = service.xacNhan(fileExcel(dong1, dong2));

        assertEquals(2, result.getThatBai());
        assertEquals(0, result.getThemThanhCong());
        assertEquals(2, result.getChiTietLoi().size());
        assertTrue(result.getChiTietLoi().get(0).contains("Email đã được sử dụng"));
    }

    @Test
    void xacNhan_loiIntegrityKhongLamHetBatchThatBai() throws Exception {
        NhanVienService nhanVienService = mock(NhanVienService.class);
        when(nhanVienService.create(any(), eq(false)))
            .thenThrow(new DataIntegrityViolationException("trung khoa chinh"));
        NhanVienRepository nvRepo = mock(NhanVienRepository.class);
        when(nvRepo.findByMaNhanVien(anyString())).thenReturn(Optional.empty());

        NhanVienImportService service = importService(nhanVienService, nvRepo, vaiTroRepository());

        NhanVienImportKetQuaResponse result = service.xacNhan(fileExcel(DONG_HOP_LE));

        assertEquals(1, result.getThatBai());
        assertEquals(1, result.getChiTietLoi().size());
    }

    @Test
    void docDong_fileRong_BaoLoi() {
        NhanVienImportService service = importService(
            nhanVienService(nhanVienRepository(), vaiTroRepository()),
            nhanVienRepository(), vaiTroRepository());

        MockMultipartFile rong = new MockMultipartFile("file", "rong.xlsx",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);
        assertThrows(IllegalArgumentException.class, () -> service.xemTruoc(rong));

        MockMultipartFile saiDinhDang = new MockMultipartFile("file", "a.txt", "text/plain",
            "nội dung".getBytes());
        assertThrows(IllegalArgumentException.class, () -> service.xemTruoc(saiDinhDang));
    }

    @Test
    void docDong_chiCoHeader_BaoLoiKhongDuLieu() throws Exception {
        NhanVienImportService service = importService(
            nhanVienService(nhanVienRepository(), vaiTroRepository()),
            nhanVienRepository(), vaiTroRepository());

        assertThrows(IllegalArgumentException.class, () -> service.xemTruoc(fileExcel()));
    }

    @Test
    void xemTruoc_vaiTroKhongHopLe_BaoLoiDong() throws Exception {
        NhanVienRepository nvRepo = nhanVienRepository();
        NhanVienImportService service = importService(
            nhanVienService(nvRepo, vaiTroRepository()), nvRepo, vaiTroRepository());

        String[] dong = DONG_HOP_LE.clone();
        dong[6] = "QUANLY";
        NhanVienImportPreviewResponse preview = service.xemTruoc(fileExcel(dong));

        assertEquals(1, preview.getDongLoi());
        assertTrue(preview.getDong().get(0).getLyDoLoi().contains("Vai trò"));
    }

    @Test
    void xemTruoc_saiThuTuCot_BaoLoiSaiCauTruc() throws Exception {
        NhanVienRepository nvRepo = nhanVienRepository();
        NhanVienImportService service = importService(
            nhanVienService(nvRepo, vaiTroRepository()), nvRepo, vaiTroRepository());

        // Đảo 2 cột "Email" và "Số điện thoại" → đọc theo vị trí sẽ nhầm dữ liệu
        String[] tieuDeSai = TIEU_DE_MAU.clone();
        tieuDeSai[2] = "Số điện thoại";
        tieuDeSai[3] = "Email";

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.xemTruoc(fileExcelTieuDe(tieuDeSai, DONG_HOP_LE)));
        assertTrue(ex.getMessage().contains("Sai cấu trúc"));
        assertTrue(ex.getMessage().contains("Email"));
    }

    @Test
    void xemTruoc_thieuCot_BaoLoiSaiCauTruc() throws Exception {
        NhanVienRepository nvRepo = nhanVienRepository();
        NhanVienImportService service = importService(
            nhanVienService(nvRepo, vaiTroRepository()), nvRepo, vaiTroRepository());

        // Tệp chỉ có 8 cột (bỏ 2 cột địa chỉ cuối)
        String[] tieuDeThieu = java.util.Arrays.copyOf(TIEU_DE_MAU, 8);
        String[] dongThieu = java.util.Arrays.copyOf(DONG_HOP_LE, 8);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.xemTruoc(fileExcelTieuDe(tieuDeThieu, dongThieu)));
        assertTrue(ex.getMessage().contains("Sai cấu trúc"));
    }

    @Test
    void xemTruoc_dungFileXuatSanPham_BaoLoiSaiCauTruc() throws Exception {
        NhanVienRepository nvRepo = nhanVienRepository();
        NhanVienImportService service = importService(
            nhanVienService(nvRepo, vaiTroRepository()), nvRepo, vaiTroRepository());

        // Hàng tiêu đề của tệp import SẢN PHÁM — dùng nhầm file xuất sẽ bị chặn ngay
        String[] tieuDeSanPham = {
            "STT", "Tên sản phẩm (*)", "Thương hiệu (*)", "Loại giày (*)",
            "Chất liệu (*)", "Kiểu dáng (*)", "Xuất xứ (*)", "Đối tượng",
            "Màu sắc (*)", "Kích cỡ (*)", "Số lượng (*)", "Giá bán (VNĐ) (*)",
            "Tính năng", "Mô tả"
        };

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
            () -> service.xemTruoc(fileExcelTieuDe(tieuDeSanPham, DONG_HOP_LE)));
        assertTrue(ex.getMessage().contains("Sai cấu trúc"));
    }

    @Test
    void xacNhan_fileSaiCauTruc_BaoLoiTruocKhiGhi() throws Exception {
        NhanVienService nhanVienService = mock(NhanVienService.class);
        NhanVienRepository nvRepo = nhanVienRepository();
        NhanVienImportService service = importService(nhanVienService, nvRepo, vaiTroRepository());

        String[] tieuDeSai = TIEU_DE_MAU.clone();
        tieuDeSai[0] = "Mã SP";

        assertThrows(IllegalArgumentException.class,
            () -> service.xacNhan(fileExcelTieuDe(tieuDeSai, DONG_HOP_LE)));
        verify(nhanVienService, org.mockito.Mockito.never()).create(any(), eq(false));
    }

    /**
     * Người dùng sửa tệp Excel rồi xác nhận lần nữa (không cần xem trước):
     * lần xác nhận phải đọc đúng nội dung MỚI NHẤT của tệp, không dùng lại
     * dữ liệu của lần xem trước hoặc lần import trước.
     */
    @Test
    void xacNhan_sauKhiSuaTeph_XacNhanLaiNhapNoiDungMoiNhat() throws Exception {
        NhanVien cu = new NhanVien();
        cu.setId(7L);
        cu.setMaNhanVien("NV01");
        cu.setTrangThai(true);

        NhanVienService nhanVienService = mock(NhanVienService.class);
        NhanVienRepository nvRepo = mock(NhanVienRepository.class);
        when(nvRepo.findByMaNhanVien("NV01")).thenReturn(Optional.of(cu));
        when(nvRepo.findById(7L)).thenReturn(Optional.of(cu));

        NhanVienImportService service = importService(nhanVienService, nvRepo, vaiTroRepository());

        // Bước 1: xem trước tệp cũ — chỉ 1 dòng NV01
        String[] dongCu = DONG_HOP_LE.clone();
        dongCu[0] = "NV01";
        service.xemTruoc(fileExcel(dongCu));

        // Bước 2: sửa tệp — thêm dòng mã trống (tự sinh) và đổi email dòng NV01
        String[] dongThem = DONG_HOP_LE.clone();
        dongThem[2] = "them-moi@example.com";
        String[] dongCapNhat = dongCu.clone();
        dongCapNhat[2] = "email-moi@example.com";
        dongCapNhat[3] = "0901234568";

        NhanVienImportKetQuaResponse result = service.xacNhan(fileExcel(dongThem, dongCapNhat));

        assertEquals(1, result.getThemThanhCong());
        assertEquals(1, result.getCapNhatThanhCong());
        assertEquals(0, result.getThatBai());
        verify(nhanVienService).create(
            org.mockito.ArgumentMatchers.argThat(r -> "them-moi@example.com".equals(r.getEmail())),
            eq(false));
        verify(nhanVienService).update(
            eq(7L),
            org.mockito.ArgumentMatchers.argThat(r -> "email-moi@example.com".equals(r.getEmail())));
    }

    /**
     * Import lại cùng tệp sau khi sửa: NV01 đã tồn tại thì được CẬP NHẬT
     * theo nội dung mới, không tạo bản ghi trùng lặp.
     */
    @Test
    void xacNhan_importLaiTungSua_NoibungMoiCapNhatKhongTrung() throws Exception {
        NhanVien cu = new NhanVien();
        cu.setId(7L);
        cu.setMaNhanVien("NV01");
        cu.setTrangThai(true);

        NhanVienService nhanVienService = mock(NhanVienService.class);
        NhanVienRepository nvRepo = mock(NhanVienRepository.class);
        when(nvRepo.findByMaNhanVien("NV01")).thenReturn(Optional.of(cu));
        when(nvRepo.findById(7L)).thenReturn(Optional.of(cu));

        NhanVienImportService service = importService(nhanVienService, nvRepo, vaiTroRepository());

        String[] dong = DONG_HOP_LE.clone();
        dong[0] = "NV01";
        dong[2] = "lan1@example.com";
        NhanVienImportKetQuaResponse lan1 = service.xacNhan(fileExcel(dong));
        assertEquals(1, lan1.getCapNhatThanhCong());
        assertEquals(0, lan1.getThemThanhCong());

        // sửa email trong tệp rồi import lại — vẫn là cập nhật, không thêm mới
        String[] dongSua = dong.clone();
        dongSua[2] = "lan2@example.com";
        NhanVienImportKetQuaResponse lan2 = service.xacNhan(fileExcel(dongSua));
        assertEquals(1, lan2.getCapNhatThanhCong());
        assertEquals(0, lan2.getThemThanhCong());
        assertEquals(0, lan2.getThatBai());

        verify(nhanVienService, org.mockito.Mockito.never()).create(any(), eq(false));
        verify(nhanVienService).update(
            eq(7L),
            org.mockito.ArgumentMatchers.argThat(r -> "lan2@example.com".equals(r.getEmail())));
    }
}
