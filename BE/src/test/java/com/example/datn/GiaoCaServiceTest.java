package com.example.datn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.example.datn.dto.CaLamViecDto;
import com.example.datn.dto.TongKetDto;
import com.example.datn.entity.HoaDon;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhuongThucThanhToan;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.CaLamViecRepository;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.repository.LichSuCaRepository;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhuongThucThanhToanRepository;
import com.example.datn.repository.VaiTroRepository;
import com.example.datn.service.GiaoCaService;
import com.example.datn.service.HoaDonService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class GiaoCaServiceTest {

    @Autowired
    private GiaoCaService giaoCaService;

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private CaLamViecRepository caLamViecRepository;

    @Autowired
    private LichSuCaRepository lichSuCaRepository;

    @Autowired
    private HoaDonRepository hoaDonRepository;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private final List<Long> idCaTao = new ArrayList<>();
    private final List<Long> idNVTao = new ArrayList<>();
    private final List<Long> idHDTao = new ArrayList<>();

    private NhanVien nv1;
    private NhanVien nv2;
    private PhuongThucThanhToan ptMat;
    private PhuongThucThanhToan ptKhongMat;

    @BeforeEach
    void setUp() {
        VaiTro vaiTro = vaiTroRepository.findById(3L).orElseGet(() -> {
            VaiTro vt = new VaiTro();
            vt.setMaVaiTro("TGC_STAFF_" + System.nanoTime());
            vt.setTenVaiTro("Test Giao Ca Staff");
            vt.setTrangThai(true);
            return vaiTroRepository.save(vt);
        });
        nv1 = taoNhanVien("testgc", vaiTro);
        nv2 = taoNhanVien("testgc", vaiTro);
        ptMat = timPhuongThuc("Tiền mặt");
        ptKhongMat = timPhuongThuc("Chuyển khoản");
        dangNhap(nv1.getId(), false);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        for (Long idHd : idHDTao) {
            jdbcTemplate.update("DELETE FROM lich_su_hoa_don WHERE id_hoa_don = ?", idHd);
            jdbcTemplate.update("DELETE FROM hoa_don WHERE id = ?", idHd);
        }
        idHDTao.clear();
        for (Long idCa : idCaTao) {
            jdbcTemplate.update("DELETE FROM lich_su_ca WHERE id_ca_lam_viec = ?", idCa);
            jdbcTemplate.update("DELETE FROM ca_lam_viec WHERE id = ?", idCa);
        }
        idCaTao.clear();
        for (Long idNv : idNVTao) {
            jdbcTemplate.update("DELETE FROM nhan_vien WHERE id = ?", idNv);
        }
        idNVTao.clear();
    }

    @Test
    void moCa_taoMoi_va_khongChoMoLanHai() {
        CaLamViecDto ca = giaoCaService.moCa(500000L, "dau ca test");
        idCaTao.add(ca.getId());

        assertThat(ca.getTrangThai()).isEqualTo("DANG_MO");
        assertThat(ca.getMaCa()).startsWith("GC-");
        assertThat(ca.getTienDauCa()).isEqualByComparingTo(new BigDecimal("500000"));
        assertThat(giaoCaService.getDangMoOf(nv1.getId())).isNotNull();

        assertThatThrownBy(() -> giaoCaService.moCa(100000L, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rs = (ResponseStatusException) ex;
                assertThat(rs.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(rs.getReason()).contains("CA_DA_MO");
            });

        jdbcTemplate.update(
            "UPDATE ca_lam_viec SET trang_thai = 'DA_KET_CA' WHERE id = ?", ca.getId());
    }

    @Test
    void ketCa_tinhDungChenhLech() {
        CaLamViecDto ca = giaoCaService.moCa(500000L, null);
        idCaTao.add(ca.getId());

        taoHoaDon("TAI_QUAY", "DA_HOAN_THANH", ptMat, new BigDecimal("200000"), ca.getId());
        taoHoaDon("TAI_QUAY", "DA_HUY", ptMat, new BigDecimal("99999"), ca.getId());
        taoHoaDon("ONLINE", "DA_HOAN_THANH", ptKhongMat, new BigDecimal("100000"), ca.getId());

        CaLamViecDto ketQua = giaoCaService.ketCa(ca.getId(), 650000L, "lech tien mat kiem ke", null);

        assertThat(ketQua.getTrangThai()).isEqualTo("DA_KET_CA");
        assertThat(ketQua.getTienDuKien()).isEqualByComparingTo(new BigDecimal("700000"));
        assertThat(ketQua.getChenhLech()).isEqualByComparingTo(new BigDecimal("-50000"));
        assertThat(ketQua.getTienMatThu()).isEqualByComparingTo(new BigDecimal("200000"));
        assertThat(ketQua.getTienKhongMat()).isEqualByComparingTo(new BigDecimal("100000"));
        assertThat(ketQua.getTienHoanMat()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(ketQua.getTongHoaDon()).isEqualTo(1);
        assertThat(ketQua.getTienThucTe()).isEqualByComparingTo(new BigDecimal("650000"));
        assertThat(ketQua.getLyDoLech()).isNotNull();

        long soBanGhiKetCa = lichSuCaRepository
            .findByCaLamViecId(ca.getId()).stream()
            .filter(l -> "KET_CA".equals(l.getHanhDong()))
            .count();
        assertThat(soBanGhiKetCa).isEqualTo(1);
    }

    @Test
    void ketCa_thieuLyDoLech() {
        CaLamViecDto ca = giaoCaService.moCa(0L, null);
        idCaTao.add(ca.getId());

        assertThatThrownBy(() -> giaoCaService.ketCa(ca.getId(), 1L, null, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rs = (ResponseStatusException) ex;
                assertThat(rs.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(rs.getReason()).contains("LY_DO_LECH_THIEU");
            });
    }

    @Test
    void ketCa_lanThuHai_bi409() {
        CaLamViecDto ca = giaoCaService.moCa(100000L, null);
        idCaTao.add(ca.getId());
        giaoCaService.ketCa(ca.getId(), 100000L, null, null);

        assertThatThrownBy(() -> giaoCaService.ketCa(ca.getId(), 100000L, null, null))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rs = (ResponseStatusException) ex;
                assertThat(rs.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                assertThat(rs.getReason()).contains("CA_DANG_KET");
            });
    }

    @Test
    void tongKet_hoanTienMat_vaOnlineKhongChan() {
        CaLamViecDto ca = giaoCaService.moCa(100000L, null);
        idCaTao.add(ca.getId());

        taoHoaDon("TAI_QUAY", "DA_HOAN_THANH", ptMat, new BigDecimal("50000"), ca.getId());
        taoHoaDon("TAI_QUAY", "DA_HOAN_TIEN", ptMat, new BigDecimal("50000"), ca.getId());
        taoHoaDon("ONLINE", "CHO_XAC_NHAN", ptKhongMat, new BigDecimal("999999"), ca.getId());

        TongKetDto tongKet = giaoCaService.tongKet(ca.getId());
        assertThat(tongKet.getTienHoanMat()).isEqualByComparingTo(new BigDecimal("50000"));
        assertThat(tongKet.getTienDuKien()).isEqualByComparingTo(new BigDecimal("100000"));

        CaLamViecDto ketQua = giaoCaService.ketCa(ca.getId(), 100000L, null, null);
        assertThat(ketQua.getTrangThai()).isEqualTo("DA_KET_CA");
    }

    @Test
    void chuyenTrangThaiPos_choPhepHoanTien() {
        HoaDon hd = taoHoaDon("TAI_QUAY", "DA_HOAN_THANH", ptMat, new BigDecimal("150000"), null);

        hoaDonService.doiTrangThai(hd.getId(), "DA_HOAN_TIEN", "hoan tai quay");

        HoaDon sau = hoaDonRepository.findById(hd.getId()).orElseThrow();
        assertThat(sau.getTrangThai()).isEqualTo("DA_HOAN_TIEN");
    }

    @Test
    void lichSu_adminThayDu_NVChiThayCuaMinh() {
        CaLamViecDto ca1 = giaoCaService.moCa(10000L, null);
        idCaTao.add(ca1.getId());
        jdbcTemplate.update(
            "UPDATE ca_lam_viec SET trang_thai = 'DA_KET_CA' WHERE id = ?", ca1.getId());

        dangNhap(nv2.getId(), false);
        CaLamViecDto ca2 = giaoCaService.moCa(20000L, null);
        idCaTao.add(ca2.getId());
        jdbcTemplate.update(
            "UPDATE ca_lam_viec SET trang_thai = 'DA_KET_CA' WHERE id = ?", ca2.getId());

        var trangQuanTri = giaoCaService.lichSu(0, 10, null, null, null, null, 1L, true);
        assertThat(trangQuanTri.getTotalElements()).isGreaterThanOrEqualTo(2);
        assertThat(trangQuanTri.getContent())
            .extracting(CaLamViecDto::getId)
            .contains(ca1.getId(), ca2.getId());

        var trangNv1 = giaoCaService.lichSu(0, 10, null, null, null, null, nv1.getId(), false);
        assertThat(trangNv1.getContent())
            .extracting(CaLamViecDto::getId)
            .contains(ca1.getId())
            .doesNotContain(ca2.getId());
    }

    // ---- helpers ----

    private NhanVien taoNhanVien(String tienTo, VaiTro vaiTro) {
        String suffix = String.valueOf(System.nanoTime());
        NhanVien nv = new NhanVien();
        nv.setVaiTro(vaiTro);
        nv.setMaNhanVien(tienTo + suffix);
        nv.setTenTaiKhoan(tienTo + suffix);
        nv.setMatKhau("x");
        nv.setTrangThai(true);
        NhanVien saved = nhanVienRepository.save(nv);
        idNVTao.add(saved.getId());
        return saved;
    }

    private PhuongThucThanhToan timPhuongThuc(String chua) {
        return phuongThucThanhToanRepository.findAll().stream()
            .filter(p -> p.getTenPhuongThuc().toLowerCase().contains(chua.toLowerCase()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Thieu phuong thuc thanh toan: " + chua));
    }

    private HoaDon taoHoaDon(
        String loaiDon, String trangThai, PhuongThucThanhToan pt, BigDecimal tien, Long idCa
    ) {
        HoaDon hd = new HoaDon();
        hd.setMaHoaDon("HDGC" + System.nanoTime());
        hd.setLoaiDon(loaiDon);
        hd.setTrangThai(trangThai);
        hd.setPhuongThucThanhToan(pt);
        hd.setTienSauGiamGia(tien);
        hd.setTongTien(tien);
        hd.setPhiVanChuyen(BigDecimal.ZERO);
        hd.setDaXoa(false);
        hd.setIdCaLamViec(idCa);
        HoaDon saved = hoaDonRepository.save(hd);
        idHDTao.add(saved.getId());
        return saved;
    }

    private static void dangNhap(Long idNhanVien, boolean admin) {
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_NHAN_VIEN"));
        if (admin) {
            authorities.add(new SimpleGrantedAuthority("ROLE_ADMIN"));
        }
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(idNhanVien, "n/a", authorities));
    }
}
