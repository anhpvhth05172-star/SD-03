package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.datn.dto.NhanVienDTO;
import com.example.datn.dto.NhanVienRequest;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import java.time.LocalDate;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

class NhanVienServiceTest {

    private static NhanVienService service(NhanVienRepository repo, VaiTroRepository vaiTroRepo) {
        return new NhanVienService(repo, vaiTroRepo, mock(PlatformTransactionManager.class));
    }

    private static VaiTroRepository vaiTroRepository() {
        VaiTro vaiTro = new VaiTro();
        vaiTro.setId(1L);
        vaiTro.setMaVaiTro("STAFF");
        vaiTro.setTenVaiTro("Nhân viên");
        VaiTroRepository repo = mock(VaiTroRepository.class);
        when(repo.findById(anyLong())).thenReturn(Optional.of(vaiTro));
        return repo;
    }

    private static NhanVienRequest yeuCau(String ten, LocalDate ngaySinh, String ma) {
        NhanVienRequest req = new NhanVienRequest();
        req.setTenTaiKhoan(ten);
        req.setMaNhanVien(ma);
        req.setMatKhau("123456");
        req.setEmail("binh@example.com");
        req.setSoDienThoai("0901234567");
        req.setGioiTinh("NU");
        req.setNgaySinh(ngaySinh);
        req.setIdVaiTro(1L);
        return req;
    }

    private static NhanVienRepository repoSach() {
        NhanVienRepository repo = mock(NhanVienRepository.class);
        when(repo.saveAndFlush(any(NhanVien.class))).thenAnswer(inv -> inv.getArgument(0));
        when(repo.save(any(NhanVien.class))).thenAnswer(inv -> inv.getArgument(0));
        return repo;
    }

    @Test
    void maTuDongThemSoThuTuKhiTrung() {
        NhanVienRepository repo = mock(NhanVienRepository.class);
        Set<String> daCo = Set.of("Anhph01", "Anhph02");
        when(repo.existsByMaNhanVien(anyString())).thenAnswer(inv -> daCo.contains(inv.getArgument(0)));
        NhanVienService service = service(repo, vaiTroRepository());

        assertEquals("Anhph03", service.maNhanVienTuDong("Phạm Hà Anh"));
        assertEquals("Namvv01", service.maNhanVienTuDong("Võ Văn Nam"));
        assertEquals("NV01", service.maNhanVienTuDong(null));
    }

    @Test
    void maTuDongTheoQuyTacYeuCau() {
        NhanVienRepository repo = mock(NhanVienRepository.class);
        NhanVienService service = service(repo, vaiTroRepository());

        assertEquals("Binhnt01", service.maNhanVienTuDong("Nguyễn Thị Bình"));
        assertEquals("Binhnv01", service.maNhanVienTuDong("Nguyễn Văn Bình"));
        assertEquals("Anhtv01", service.maNhanVienTuDong("Trần Văn Anh"));
        assertEquals("Anhph01", service.maNhanVienTuDong("Phạm Hà Anh"));
    }

    @Test
    void taoNhanVienDungMaDeXuat() {
        NhanVienRepository repo = repoSach();
        NhanVienService service = service(repo, vaiTroRepository());

        NhanVienDTO dto = service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(30), ""));
        assertEquals("Binhnt01", dto.getMaNhanVien());
    }

    @Test
    void taoNhanVienBoQuaMaTrungVaSinhMaKeTiep() {
        NhanVienRepository repo = repoSach();
        when(repo.existsByMaNhanVien(anyString())).thenAnswer(inv -> {
            String ma = inv.getArgument(0);
            return "Binhnt01".equals(ma) || "Binhnt02".equals(ma);
        });
        NhanVienService service = service(repo, vaiTroRepository());

        NhanVienDTO dto = service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(30), ""));
        assertEquals("Binhnt03", dto.getMaNhanVien());

        // FE gửi sẵn mã đã tồn tại -> backend vẫn sinh mã kế tiếp.
        NhanVienDTO dto2 = service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(30), "Binhnt01"));
        assertEquals("Binhnt03", dto2.getMaNhanVien());
    }

    @Test
    void taoLaiKhiHaiYeuCauCungLucGianhCungMa() {
        NhanVienRepository repo = repoSach();
        Set<String> daCo = ConcurrentHashMap.newKeySet();
        AtomicBoolean daChenLanDau = new AtomicBoolean(true);
        when(repo.existsByMaNhanVien(anyString())).thenAnswer(inv -> daCo.contains(inv.getArgument(0)));
        when(repo.saveAndFlush(any(NhanVien.class))).thenAnswer(inv -> {
            NhanVien nv = inv.getArgument(0);
            if (daChenLanDau.compareAndSet(true, false)) {
                // Yêu cầu khác đã ghi cùng mã ngay sau khi mình kiểm tra -> ràng buộc unique chặn lại.
                daCo.add(nv.getMaNhanVien());
                throw new DataIntegrityViolationException("Cannot insert duplicate key row");
            }
            if (!daCo.add(nv.getMaNhanVien())) {
                throw new DataIntegrityViolationException("Cannot insert duplicate key row");
            }
            return nv;
        });
        NhanVienService service = service(repo, vaiTroRepository());

        NhanVienDTO dto = service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(30), ""));
        assertEquals("Binhnt02", dto.getMaNhanVien());
    }

    @Test
    void tuChoiNhanVienChuaDu18Tuoi() {
        NhanVienService service = service(repoSach(), vaiTroRepository());

        LocalDate qua1Ngay = LocalDate.now().minusYears(18).plusDays(1);
        IllegalArgumentException loi17 = assertThrows(IllegalArgumentException.class,
            () -> service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(17), "")));
        assertEquals(NhanVienService.THONG_BAO_TUOI, loi17.getMessage());

        IllegalArgumentException loiChuaDu1Ngay = assertThrows(IllegalArgumentException.class,
            () -> service.create(yeuCau("Nguyễn Thị Bình", qua1Ngay, "")));
        assertEquals(NhanVienService.THONG_BAO_TUOI, loiChuaDu1Ngay.getMessage());
    }

    @Test
    void chapNhanNhanVienVuaDu18Tuoi() {
        NhanVienService service = service(repoSach(), vaiTroRepository());

        NhanVienDTO dto = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(18), ""));
        assertEquals("Binhnt01", dto.getMaNhanVien());

        NhanVienDTO dtoCungNgay = service.create(
            yeuCau("Trần Văn Anh", LocalDate.now().minusYears(18).minusDays(1), ""));
        assertEquals("Anhtv01", dtoCungNgay.getMaNhanVien());
    }

    @Test
    void tuChoiNgaySinhTuongLaiVaKhongHopLe() {
        NhanVienService service = service(repoSach(), vaiTroRepository());

        IllegalArgumentException loiTuongLai = assertThrows(IllegalArgumentException.class,
            () -> service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().plusDays(1), "")));
        assertEquals("Ngày sinh không được ở tương lai", loiTuongLai.getMessage());

        IllegalArgumentException loiRong = assertThrows(IllegalArgumentException.class,
            () -> service.create(yeuCau("Nguyễn Thị Bình", null, "")));
        assertEquals("Ngày sinh không được để trống", loiRong.getMessage());
    }

    @Test
    void tuChoiSuaNgaySinhThanhChuaDu18Tuoi() {
        NhanVienService service = service(repoSach(), vaiTroRepository());

        NhanVienRequest req = yeuCau("Lê Thị Hoa", LocalDate.now().minusYears(17), "Binhnt01");
        IllegalArgumentException loi = assertThrows(IllegalArgumentException.class,
            () -> service.update(9L, req));
        assertEquals(NhanVienService.THONG_BAO_TUOI, loi.getMessage());

        // Chưa đủ 18 tuổi một ngày cũng bị chặn khi gọi API cập nhật trực tiếp.
        NhanVienRequest qua1Ngay = yeuCau("Lê Thị Hoa",
            LocalDate.now().minusYears(18).plusDays(1), "Binhnt01");
        assertEquals(NhanVienService.THONG_BAO_TUOI,
            assertThrows(IllegalArgumentException.class, () -> service.update(9L, qua1Ngay))
                .getMessage());
    }

    @Test
    void thongBaoTuoiDungChuoiYeuCau() {
        assertEquals("Nhân viên phải đủ 18 tuổi.", NhanVienService.THONG_BAO_TUOI);
    }

    @Test
    void khongDoiMaDaLuuKhiSuaTen() {
        NhanVienRepository repo = repoSach();
        when(repo.existsByMaNhanVienAndIdNot(anyString(), anyLong())).thenReturn(false);
        NhanVienService service = service(repo, vaiTroRepository());

        NhanVien cu = new NhanVien();
        cu.setId(9L);
        cu.setMaNhanVien("Binhnt01");
        when(repo.findById(9L)).thenReturn(Optional.of(cu));

        NhanVienRequest req = yeuCau("Lê Thị Hoa", LocalDate.now().minusYears(30), "Binhnt01");
        NhanVienDTO dto = service.update(9L, req);
        assertEquals("Binhnt01", dto.getMaNhanVien());
        assertTrue(dto.getTenTaiKhoan().equals("Lê Thị Hoa"));
    }
}
