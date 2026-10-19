package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.datn.dto.Chat.HoiThoaiDTO;
import com.example.datn.dto.Chat.TinNhanDTO;
import com.example.datn.entity.HoiThoai;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.TinNhan;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.HoiThoaiRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.TinNhanRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ChatHopSinhChungTest {

    private final HoiThoaiRepository hoiThoaiRepository = mock(HoiThoaiRepository.class);
    private final TinNhanRepository tinNhanRepository = mock(TinNhanRepository.class);
    private final KhachHangRepository khachHangRepository = mock(KhachHangRepository.class);
    private final NhanVienRepository nhanVienRepository = mock(NhanVienRepository.class);

    private final ChatService service = new ChatService(
        hoiThoaiRepository,
        tinNhanRepository,
        khachHangRepository,
        nhanVienRepository
    );

    private NhanVien nhanVien(long id, String tenTk) {
        NhanVien nv = new NhanVien();
        nv.setId(id);
        nv.setTenTaiKhoan(tenTk);
        VaiTro vt = new VaiTro();
        vt.setId(3L);
        vt.setMaVaiTro("STAFF");
        nv.setVaiTro(vt);
        nv.setTrangThai(true);
        return nv;
    }

    private HoiThoai hoiThoaiDangMo(NhanVien phuTrach) {
        HoiThoai h = new HoiThoai();
        h.setId(10L);
        h.setLoai(HoiThoai.LOAI_NHAN_VIEN);
        h.setTrangThai(HoiThoai.TRANG_THAI_DANG_MO);
        h.setNhanVienPhuTrach(phuTrach);
        h.setTongTinKhach(1);
        h.setTongTinNhanVien(1);
        return h;
    }

    private void chuanBiGuiTin(HoiThoai h) {
        when(hoiThoaiRepository.findByIdAndLoai(10L, HoiThoai.LOAI_NHAN_VIEN)).thenReturn(Optional.of(h));
        when(tinNhanRepository.save(any(TinNhan.class))).thenAnswer(inv -> inv.getArgument(0));
        when(hoiThoaiRepository.save(any(HoiThoai.class))).thenAnswer(inv -> inv.getArgument(0));
        when(nhanVienRepository.findById(4L)).thenReturn(Optional.of(nhanVien(4L, "duc")));
        when(nhanVienRepository.findById(5L)).thenReturn(Optional.of(nhanVien(5L, "linh")));
    }

    @Test
    void guiTinNhanVien_NVKhacVoiNguoiPhuTrach_VanGuiDuoc() {
        HoiThoai h = hoiThoaiDangMo(nhanVien(4L, "duc"));
        chuanBiGuiTin(h);

        assertThatCode(() -> service.guiTinNhanVien(10L, 5L, "Minh tra loi ban nhe"))
            .doesNotThrowAnyException();
    }

    @Test
    void tinNhanVien_HienTenTaiKhoanNguoiTraLoi() {
        HoiThoai h = hoiThoaiDangMo(nhanVien(4L, "duc"));
        chuanBiGuiTin(h);

        TinNhanDTO dto = service.guiTinNhanVien(10L, 4L, "Tra loi tu duc");

        assertThat(dto.getTenNguoiGui()).isEqualTo("duc");
    }

    @Test
    void layHoiThoaiKhachDangNhap_DatTenTaiKhoanLamTenKhach() {
        KhachHang kh = new KhachHang();
        kh.setId(2L);
        kh.setTenTaiKhoan("smokekh01");
        kh.setTenKhachHang("Nguyen Van A");
        when(khachHangRepository.findById(2L)).thenReturn(Optional.of(kh));
        when(hoiThoaiRepository.save(any(HoiThoai.class))).thenAnswer(inv -> inv.getArgument(0));

        HoiThoaiDTO dto = service.layHoiThoaiNhanVien(2L, null);

        assertThat(dto.getTenKhach()).isEqualTo("smokekh01");
    }

    @Test
    void layTinNhanVien_NVKhacPhuTrach_VanXemDuoc() {
        HoiThoai h = hoiThoaiDangMo(nhanVien(4L, "duc"));
        when(hoiThoaiRepository.findByIdAndLoai(10L, HoiThoai.LOAI_NHAN_VIEN)).thenReturn(Optional.of(h));
        TinNhan t = new TinNhan();
        t.setId(1L);
        t.setHoiThoai(h);
        t.setLoaiNguoiGui(TinNhan.GUI_KHACH);
        t.setNoiDung("Can mua giay");
        when(tinNhanRepository.timMoiHon(10L, 0L)).thenReturn(java.util.List.of(t));

        assertThat(service.layTinNhanVien(10L, 5L, 0L)).hasSize(1);
    }
}
