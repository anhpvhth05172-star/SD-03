package com.example.datn.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.KhachHang.KhachHangDTO;
import com.example.datn.dto.KhachHang.KhachHangRequest;
import com.example.datn.entity.KhachHang;
import com.example.datn.repository.DiaChiKhachHangRepository;
import com.example.datn.repository.KhachHangRepository;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
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
        return service(null);
    }

    private KhachHangService service(EmailService emailService) {
        when(khachHangRepository.saveAndFlush(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(khachHangRepository.save(any(KhachHang.class))).thenAnswer(inv -> inv.getArgument(0));
        when(khachHangRepository.thongKeTheoKhachHang(any())).thenReturn(List.of());
        when(diaChiRepository.findByKhachHangIdOrderByMacDinhDescIdDesc(any())).thenReturn(List.of());
        when(diaChiRepository.findFirstByKhachHangIdAndMacDinhTrue(any())).thenReturn(Optional.empty());
        when(diaChiRepository.save(any(com.example.datn.entity.DiaChiKhachHang.class)))
            .thenAnswer(inv -> inv.getArgument(0));
        return new KhachHangService(khachHangRepository, diaChiRepository, emailService,
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
        req.setTinhThanhPho("Thành phố Hà Nội");
        req.setPhuong("Phường Kim Mã");
        req.setDiaChiCuThe("Số 1, đường Phố Huế");
        return req;
    }

    @Test
    void maTuDongTheoQuyTacYeuCau() {
        KhachHangService service = service();
        // Chưa có mã trùng -> dùng mã gốc không hậu tố số.
        assertEquals("Binhnt", service.maKhachHangTuDong("Nguyễn Thị Bình"));
        assertEquals("Binhnv", service.maKhachHangTuDong("Nguyễn Văn Bình"));
        assertEquals("Anhtv", service.maKhachHangTuDong("Trần Văn Anh"));
        assertEquals("Anhph", service.maKhachHangTuDong("Phạm Hà Anh"));
        assertEquals("NV", service.maKhachHangTuDong(null));
    }

    @Test
    void maTuDongTangSoKhiMaGocDaTonTai() {
        KhachHangService service = service();
        when(khachHangRepository.existsByMaKhachHang(anyString()))
            .thenAnswer(inv -> Set.of("Binhnt", "Binhnt02").contains(inv.getArgument(0)));

        // Mã gốc đã tồn tại -> hậu số bắt đầu từ 02 (không dùng 01).
        assertEquals("Binhnt03", service.maKhachHangTuDong("Nguyễn Thị Bình"));

        when(khachHangRepository.existsByMaKhachHang(anyString()))
            .thenAnswer(inv -> Set.of("Binhnt").contains(inv.getArgument(0)));
        assertEquals("Binhnt02", service.maKhachHangTuDong("Nguyễn Thị Bình"));
    }

    @Test
    void maKhachHangTangSoKhiChiMaGocDaTonTai() {
        // Mã khách hàng chỉ kiểm tra trong bảng khách hàng, không phụ thuộc mã nhân viên.
        KhachHangService service = service();
        when(khachHangRepository.existsByMaKhachHang(anyString()))
            .thenAnswer(inv -> "Anhph".equals(inv.getArgument(0)));
        assertEquals("Anhph02", service.maKhachHangTuDong("Phạm Hà Anh"));
    }

    @Test
    void taoKhachHangDungMaDeXuat() {
        KhachHangService service = service();
        KhachHangDTO dto = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), ""));
        assertEquals("Binhnt", dto.getMaKhachHang());
    }

    @Test
    void taoKhachHangBoQuaMaTrungVaSinhMaKeTiep() {
        KhachHangService service = service();
        when(khachHangRepository.existsByMaKhachHang(anyString()))
            .thenAnswer(inv -> Set.of("Binhnt", "Binhnt02").contains(inv.getArgument(0)));

        KhachHangDTO dto = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), ""));
        assertEquals("Binhnt03", dto.getMaKhachHang());

        // FE gửi sẵn mã đã tồn tại -> backend vẫn sinh mã kế tiếp.
        KhachHangDTO dto2 = service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "Binhnt"));
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
        assertEquals("Binhnt", dto.getMaKhachHang());

        KhachHangDTO dtoCungNgay = service.create(
            yeuCau("Trần Văn Anh", LocalDate.now().minusYears(16).minusDays(1), ""));
        assertEquals("Anhtv", dtoCungNgay.getMaKhachHang());
    }

    @Test
    void tuChoiNgaySinhTuongLaiVaGiuaNgaySinhBatBuoc() {
        KhachHangService service = service();

        IllegalArgumentException loiTuongLai = assertThrows(IllegalArgumentException.class,
            () -> service.create(yeuCau("Nguyễn Thị Bình", LocalDate.now().plusDays(1), "")));
        assertEquals("Ngày sinh không được ở tương lai", loiTuongLai.getMessage());

        // Ngày sinh của khách hàng vẫn là tùy chọn như trước đây.
        KhachHangDTO dto = service.create(yeuCau("Nguyễn Thị Bình", null, ""));
        assertEquals("Binhnt", dto.getMaKhachHang());
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
            .getMessage().startsWith("Số điện thoại phải bắt đầu bằng số 0"));

        KhachHangRequest sdt9So = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        sdt9So.setSoDienThoai("090123456");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.create(sdt9So))
            .getMessage().startsWith("Số điện thoại phải bắt đầu bằng số 0"));

        KhachHangRequest sdt10So = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        sdt10So.setSoDienThoai("0901234567");
        assertEquals("0901234567", service.create(sdt10So).getSoDienThoai());

        KhachHangRequest maSai = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "1abc");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.create(maSai))
            .getMessage().startsWith("Mã khách hàng không hợp lệ"));
    }

    @Test
    void taoKhachHangDeTrongDiaChiVanTaoDuocVaKhongSinhSoDiaChi() {
        KhachHangService service = service();

        KhachHangRequest khongDiaChi = yeuCau("Nguyễn Thơ Bính", LocalDate.now().minusYears(20), "");
        khongDiaChi.setTinhThanhPho("  ");
        khongDiaChi.setPhuong(null);
        khongDiaChi.setDiaChiCuThe("");

        KhachHangDTO dto = service.create(khongDiaChi);

        assertEquals("Nguyễn Thơ Bính", dto.getTenKhachHang());
        verify(diaChiRepository, never()).save(any(com.example.datn.entity.DiaChiKhachHang.class));
    }

    @Test
    void capNhatDeTrongDiaChiKhongDongVaoSoDiaChi() {
        KhachHangService service = service();

        KhachHang daCo = new KhachHang();
        daCo.setId(9L);
        daCo.setTenKhachHang("Nguyễn Thơ Bính");
        when(khachHangRepository.findById(9L)).thenReturn(Optional.of(daCo));

        KhachHangRequest suaKhongDiaChi = yeuCau("Nguyễn Thơ Bính", LocalDate.now().minusYears(20), "");
        suaKhongDiaChi.setTinhThanhPho(null);
        suaKhongDiaChi.setPhuong("  ");
        suaKhongDiaChi.setDiaChiCuThe(null);

        service.update(9L, suaKhongDiaChi);

        verify(diaChiRepository, never()).save(any(com.example.datn.entity.DiaChiKhachHang.class));
        verify(diaChiRepository, never())
            .findFirstByKhachHangIdAndMacDinhTrue(any());
    }

    @Test
    void vanTaoDiaChiMacDinhKhiNhapDiaChi() {
        KhachHangService service = service();

        KhachHangRequest coDiaChi = yeuCau("Nguyễn Thơ Bính", LocalDate.now().minusYears(20), "");
        service.create(coDiaChi);

        verify(diaChiRepository).save(any(com.example.datn.entity.DiaChiKhachHang.class));
    }

    @Test
    void tuChoiTaoKhachHangThieuGioiTinh() {
        KhachHangService service = service();

        KhachHangRequest req = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        req.setGioiTinh("  ");
        assertEquals("Giới tính không được để trống",
            assertThrows(IllegalArgumentException.class, () -> service.create(req)).getMessage());
    }

    @Test
    void luuVaTraVeSoCccdKhiTao() {
        KhachHangService service = service();

        KhachHangRequest req = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        req.setSoCccd("012345678901");

        assertEquals("012345678901", service.create(req).getSoCccd());

        KhachHangRequest khongCccd = yeuCau("Trần Văn Anh", LocalDate.now().minusYears(20), "");
        assertNull(service.create(khongCccd).getSoCccd());
    }

    @Test
    void capNhatVaXoaSoCccd() {
        KhachHangService service = service();
        KhachHang daCo = new KhachHang();
        daCo.setId(9L);
        daCo.setTenKhachHang("Nguyễn Thị Bình");
        when(khachHangRepository.findById(9L)).thenReturn(Optional.of(daCo));

        KhachHangRequest req = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "Binhnt01");
        req.setSoCccd("012345678901");
        assertEquals("012345678901", service.update(9L, req).getSoCccd());
        assertEquals("012345678901", daCo.getSoCccd());

        // Gửi chuỗi rỗng là cách FE xóa số CCCD -> không lưu chuỗi rỗng.
        req.setSoCccd("   ");
        assertNull(service.update(9L, req).getSoCccd());
        assertNull(daCo.getSoCccd());
    }

    @Test
    void tuChoiSoCccdSaiDinhDang() {
        KhachHangService service = service();

        KhachHangRequest chuChu = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        chuChu.setSoCccd("abc123");
        assertTrue(assertThrows(IllegalArgumentException.class, () -> service.create(chuChu))
            .getMessage().startsWith("Số CCCD không hợp lệ"));

        KhachHangRequest quaNgan = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        quaNgan.setSoCccd("123");
        assertThrows(IllegalArgumentException.class, () -> service.create(quaNgan));

        KhachHangRequest quaDai = yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "");
        quaDai.setSoCccd("012345678901234");
        assertThrows(IllegalArgumentException.class, () -> service.create(quaDai));
    }

    @Test
    void baoTrangThaiGuiEmailTheoKetQuaThucTe() {
        EmailService emailService = mock(EmailService.class);
        KhachHangService service = service(emailService);

        // Nhà cung cấp mail chấp nhận -> DTO báo true.
        when(emailService.guiMailDangKyThanhCong(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(true);
        assertEquals(Boolean.TRUE, service.create(
            yeuCau("Nguyễn Thị Bình", LocalDate.now().minusYears(20), "")).getEmailGuiThanhCong());

        // Gửi thất bại -> DTO báo false, tuyệt đối không báo thành công giả.
        when(emailService.guiMailDangKyThanhCong(anyString(), anyString(), anyString(), anyString(), anyString()))
            .thenReturn(false);
        assertEquals(Boolean.FALSE, service.create(
            yeuCau("Trần Văn Anh", LocalDate.now().minusYears(20), "")).getEmailGuiThanhCong());

        // Không cấu hình dịch vụ email -> báo false.
        assertEquals(Boolean.FALSE, service(null).create(
            yeuCau("Võ Văn Nam", LocalDate.now().minusYears(20), "")).getEmailGuiThanhCong());
    }
}
