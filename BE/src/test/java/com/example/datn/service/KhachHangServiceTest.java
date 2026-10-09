package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.datn.dto.KhachHangDTO;
import com.example.datn.dto.KhachHangRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.repository.DiaChiKhachHangRepository;
import com.example.datn.repository.KhachHangRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.PlatformTransactionManager;

class KhachHangServiceTest {

    private final DiaChiKhachHangRepository diaChiRepository = mock(DiaChiKhachHangRepository.class);
    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);

    private KhachHangService service() {
        when(khachHangRepository.saveAndFlush(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(khachHangRepository.thongKeTheoKhachHang(any())).thenReturn(List.of());
        when(diaChiRepository.findByKhachHangIdOrderByMacDinhDescIdDesc(any())).thenReturn(List.of());
        return new KhachHangService(khachHangRepository, diaChiRepository,
            mock(PlatformTransactionManager.class));
    }

    private static KhachHangRequest yeuCau(String ten, LocalDate ngaySinh, String ma) {
        KhachHangRequest req = new KhachHangRequest();
        req.setTenKhachHang(ten);
        req.setMaKhachHang(ma);
        req.setEmail("kh@example.com");
        req.setSoDienThoai("0901234567");
        req.setGioiTinh("NU");
        req.setNgaySinh(ngaySinh);
        return req;
    }

    @Test
    void maTuDongTheoQuyTacYeuCau() {
        KhachHangService service = service();
        assertEquals("Binhnt01", service.maKhachHangTuDong("Nguyễn Thị Bình"));
        assertEquals("Binhnv01", service.maKhachHangTuDong("Nguyễn Văn Bình"));
        assertEquals("Anhtv01", service.maKhachHangTuDong("Trần Văn Anh"));
        assertEquals("Anhph01", service.maKhachHangTuDong("Phạm Hà Anh"));
        assertEquals("NV01", service.maKhachHangTuDong(null));
    }

    @Test
    void maTuDongTangSoKhiMaGocDaTonTai() {
        KhachHangService service = service();
        when(khachHangRepository.existsByMaKhachHang(anyString()))
            .thenAnswer(inv -> Set.of("Binhnt01", "Binhnt02").contains(inv.getArgument(0)));

        assertEquals("Binhnt03", service.maKhachHangTuDong("Nguyễn Thị Bình"));
    }

    @Test
    void taoKhachHangDungMaDeXuat() {
        KhachHangService service = service();
        KhachHangDTO dto = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), ""));
        assertEquals("Binhnt01", dto.getMaKhachHang());
    }

    @Test
    void taoKhachHangBoQuaMaTrungVaSinhMaKeTiep() {
        KhachHangService service = service();
        when(khachHangRepository.existsByMaKhachHang(anyString()))
            .thenAnswer(inv -> Set.of("Binhnt01", "Binhnt02").contains(inv.getArgument(0)));

        KhachHangDTO dto = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), ""));
        assertEquals("Binhnt03", dto.getMaKhachHang());

        // FE gửi sẵn mã đã tồn tại -> backend vẫn sinh mã kế tiếp.
        KhachHangDTO dto2 = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "Binhnt01"));
        assertEquals("Binhnt03", dto2.getMaKhachHang());
    }

    @Test
    void taoLaiKhiHaiYeuCauCungLucGianhCungMa() {
        KhachHangService service = service();
        Set<String> daCo = ConcurrentHashMap.newKeySet();
        AtomicBoolean daChenLanDau = new AtomicBoolean(true);
        when(khachHangRepository.existsByMaKhachHang(anyString()))
            .thenAnswer(inv -> daCo.contains(inv.getArgument(0)));
        when(khachHangRepository.saveAndFlush(any(KhachHang.class))).thenAnswer(inv -> {
            KhachHang kh = inv.getArgument(0);
            if (daChenLanDau.compareAndSet(true, false)) {
                daCo.add(kh.getMaKhachHang());
                throw new DataIntegrityViolationException("Cannot insert duplicate key row");
            }
            if (!daCo.add(kh.getMaKhachHang())) {
                throw new DataIntegrityViolationException("Cannot insert duplicate key row");
            }
            return kh;
        });

        KhachHangDTO dto = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), ""));
        assertEquals("Binhnt02", dto.getMaKhachHang());
    }

    @Test
    void tuChoiKhachHangChuaDu16Tuoi() {
        KhachHangService service = service();

        IllegalArgumentException loi15 = assertThrows(IllegalArgumentException.class,
            () -> service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(15), "")));
        assertEquals(KhachHangService.THONG_BAO_TUOI, loi15.getMessage());

        IllegalArgumentException loiChuaDu1Ngay = assertThrows(IllegalArgumentException.class,
            () -> service.create(yeuCau("Nguyễn Thị Bình",
                LocalDate.now().minusYears(16).plusDays(1), "")));
        assertEquals(KhachHangService.THONG_BAO_TUOI, loiChuaDu1Ngay.getMessage());
    }

    @Test
    void chapNhanKhachHangVuaDu16Tuoi() {
        KhachHangService service = service();

        KhachHangDTO dto = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(16), ""));
        assertEquals("Binhnt01", dto.getMaKhachHang());

        KhachHangDTO dtoCungNgay = service.create(
            yeuCau("Trần Văn Anh", LocalDate.now().minusYears(16).minusDays(1), ""));
        assertEquals("Anhtv01", dtoCungNgay.getMaKhachHang());
    }

    @Test
    void tuChoiNgaySinhTuongLaiVaGiuaNgaySinhBatBuoc() {
        KhachHangService service = service();

        IllegalArgumentException loiTuongLai = assertThrows(IllegalArgumentException.class,
            () -> service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().plusDays(1), "")));
        assertEquals("Ngày sinh không được ở tương lai", loiTuongLai.getMessage());

        // Ngày sinh của khách hàng vẫn là tùy chọn như trước đây.
        KhachHangDTO dto = service.create(yeuCau("Nguyễn Thị Bình", null, ""));
        assertEquals("Binhnt01", dto.getMaKhachHang());
    }

    @Test
    void thongBaoTuoiDungChuoiYeuCau() {
        assertEquals("Khách hàng phải đủ 16 tuổi.", KhachHangService.THONG_BAO_TUOI);
    }

    @Test
    void tuChoiSuaNgaySinhThanhChuaDu16Tuoi() {
        KhachHangService service = service();

        KhachHangRequest req = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(15), "Binhnt01");
        IllegalArgumentException loi = assertThrows(IllegalArgumentException.class,
            () -> service.update(9L, req));
        assertEquals(KhachHangService.THONG_BAO_TUOI, loi.getMessage());
    }

    @Test
    void giuNguyenCacValidationKhac() {
        KhachHangService service = service();

        KhachHangRequest khongEmail = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        khongEmail.setEmail("  ");
        assertEquals("Email không được để trống",
            assertThrows(IllegalArgumentException.class, () -> service.create(khongEmail)).getMessage());

        KhachHangRequest sdtSai = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        sdtSai.setSoDienThoai("12345");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.create(sdtSai))
            .getMessage().startsWith("Số điện thoại không hợp lệ"));

        KhachHangRequest maSai = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "1abc");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.create(maSai))
            .getMessage().startsWith("Mã khách hàng không hợp lệ"));
    }
}
