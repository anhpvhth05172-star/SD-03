package com.example.datn;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.datn.dto.CaLamViecDto;
import com.example.datn.dto.HoaDon.ChiTietHoaDonRequest;
import com.example.datn.dto.HoaDon.HoaDonDTO;
import com.example.datn.dto.HoaDon.HoaDonRequest;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.entity.NhanVien;
import com.example.datn.entity.PhuongThucThanhToan;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.PhuongThucThanhToanRepository;
import com.example.datn.repository.VaiTroRepository;
import com.example.datn.service.GHNService;
import com.example.datn.service.GiaoCaService;
import com.example.datn.service.HoaDonService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
class HoaDonPosCaLamViecTest {

    @Autowired
    private HoaDonService hoaDonService;

    @Autowired
    private GiaoCaService giaoCaService;

    @Autowired
    private NhanVienRepository nhanVienRepository;

    @Autowired
    private VaiTroRepository vaiTroRepository;

    @Autowired
    private PhuongThucThanhToanRepository phuongThucThanhToanRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private GHNService ghnService;

    private final List<Long> idCaTao = new ArrayList<>();
    private final List<Long> idNVTao = new ArrayList<>();

    private Long idVaiTroTao;
    private NhanVien nv;
    private PhuongThucThanhToan pt;
    private Long idSpct;
    private Integer soLuongGocSpct;

    @BeforeEach
    void setUp() {
        idVaiTroTao = null;
        VaiTro vaiTro = taoHayTimVaiTro();
        nv = taoNhanVien("testpos", vaiTro);
        pt = timPhuongThuc("Tiền mặt");
        chuanBiSpct();
        dangNhap(nv.getId());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        List<Long> idHdTest = jdbcTemplate.queryForList(
            "SELECT id FROM hoa_don WHERE ma_hoa_don LIKE 'HDPOST3%'", Long.class);
        for (Long idHd : idHdTest) {
            jdbcTemplate.update("DELETE FROM lich_su_hoa_don WHERE id_hoa_don = ?", idHd);
            jdbcTemplate.update("DELETE FROM chi_tiet_hoa_don WHERE id_hoa_don = ?", idHd);
            jdbcTemplate.update("DELETE FROM hoa_don WHERE id = ?", idHd);
        }
        for (Long idCa : idCaTao) {
            jdbcTemplate.update("DELETE FROM lich_su_ca WHERE id_ca_lam_viec = ?", idCa);
            jdbcTemplate.update("DELETE FROM ca_lam_viec WHERE id = ?", idCa);
        }
        idCaTao.clear();
        if (idSpct != null && soLuongGocSpct != null) {
            jdbcTemplate.update(
                "UPDATE san_pham_chi_tiet SET so_luong = ? WHERE id = ?",
                soLuongGocSpct, idSpct);
        }
        for (Long idNv : idNVTao) {
            jdbcTemplate.update("DELETE FROM nhan_vien WHERE id = ?", idNv);
        }
        idNVTao.clear();
        if (idVaiTroTao != null) {
            jdbcTemplate.update("DELETE FROM vai_tro WHERE id = ?", idVaiTroTao);
            idVaiTroTao = null;
        }
    }

    @Test
    void taoHdPos_khongMoCa_biChan_CA_CHUA_MO() {
        long soHdTruoc = demHoaDon();

        assertThatThrownBy(() -> hoaDonService.create(yeuCauTaiQuay()))
            .isInstanceOf(ResponseStatusException.class)
            .satisfies(ex -> {
                ResponseStatusException rs = (ResponseStatusException) ex;
                assertThat(rs.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(rs.getReason()).contains("CA_CHUA_MO");
            });

        assertThat(demHoaDon()).isEqualTo(soHdTruoc);
    }

    @Test
    void taoHdPos_coMoCa_stampIdCa() {
        CaLamViecDto ca = giaoCaService.moCa(100000L, null);
        idCaTao.add(ca.getId());

        HoaDonDTO dto = hoaDonService.create(yeuCauTaiQuay());

        Long idCaHd = idCaLamViecCua(dto.getId());
        assertThat(idCaHd).isEqualTo(ca.getId());
    }

    @Test
    void idNhanVien_biGhiDeTuSession() {
        CaLamViecDto ca = giaoCaService.moCa(100000L, null);
        idCaTao.add(ca.getId());

        HoaDonRequest req = yeuCauTaiQuay();
        req.setIdNhanVien(999999L);
        HoaDonDTO dto = hoaDonService.create(req);

        Long idNvHd = idNhanVienCua(dto.getId());
        assertThat(idNvHd).isEqualTo(nv.getId());
    }

    @Test
    void hdOnline_khongChan_khongStamp() {
        when(ghnService.tinhPhi(any(ShippingFeeRequest.class))).thenReturn(30000);

        HoaDonDTO dto = hoaDonService.create(yeuCauOnline());

        Long idCaHd = idCaLamViecCua(dto.getId());
        assertThat(idCaHd).isNull();
    }

    @Test
    void taoHdPos_khongDangNhap_khongChan_khongStamp() {
        CaLamViecDto ca = giaoCaService.moCa(100000L, null);
        idCaTao.add(ca.getId());
        SecurityContextHolder.clearContext();

        HoaDonDTO dto = hoaDonService.create(yeuCauTaiQuay());

        assertThat(idCaLamViecCua(dto.getId())).isNull();
        assertThat(idNhanVienCua(dto.getId())).isEqualTo(nv.getId());
    }

    // ---- helpers ----

    private HoaDonRequest yeuCauTaiQuay() {
        HoaDonRequest req = new HoaDonRequest();
        req.setMaHoaDon("HDPOST3" + System.nanoTime());
        req.setLoaiDon("TAI_QUAY");
        req.setTrangThai("CHO_XAC_NHAN");
        req.setIdPhuongThucThanhToan(pt.getId());
        req.setIdNhanVien(nv.getId());
        req.setTenKhachHang("Khach Test");
        req.setSoDienThoaiKhachHang("0912345678");
        ChiTietHoaDonRequest dong = new ChiTietHoaDonRequest();
        dong.setIdSanPhamChiTiet(idSpct);
        dong.setSoLuong(1);
        dong.setDonGia(new BigDecimal("100000"));
        req.setChiTiet(List.of(dong));
        return req;
    }

    private HoaDonRequest yeuCauOnline() {
        HoaDonRequest req = yeuCauTaiQuay();
        req.setMaHoaDon("HDPOST3" + System.nanoTime());
        req.setLoaiDon("ONLINE");
        req.setDiaChiNhanHang("12 Le Loi, Phuong Ben Nghe, Quan 1, TP Ho Chi Minh");
        req.setToProvinceId(202);
        req.setToDistrictId(1442);
        req.setToWardCode("20107");
        return req;
    }

    private Long idCaLamViecCua(Long idHd) {
        return jdbcTemplate.queryForObject(
            "SELECT id_ca_lam_viec FROM hoa_don WHERE id = ?", Long.class, idHd);
    }

    private Long idNhanVienCua(Long idHd) {
        return jdbcTemplate.queryForObject(
            "SELECT id_nhan_vien FROM hoa_don WHERE id = ?", Long.class, idHd);
    }

    private long demHoaDon() {
        Long n = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM hoa_don", Long.class);
        return n == null ? 0 : n;
    }

    private void chuanBiSpct() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
            "SELECT TOP 1 s.id, s.so_luong FROM san_pham_chi_tiet s "
                + "JOIN san_pham p ON p.id = s.id_san_pham "
                + "WHERE s.so_luong >= 1 AND s.trang_thai = 1 AND p.trang_thai = 1 "
                + "ORDER BY s.id");
        if (rows.isEmpty()) {
            throw new IllegalStateException("Khong tim thay san_pham_chi_tiet dung duoc trong DB");
        }
        idSpct = ((Number) rows.get(0).get("id")).longValue();
        soLuongGocSpct = ((Number) rows.get(0).get("so_luong")).intValue();
    }

    private VaiTro taoHayTimVaiTro() {
        VaiTro coSan = vaiTroRepository.findByMaVaiTro("STAFF");
        if (coSan != null) {
            return coSan;
        }
        VaiTro hienTai = vaiTroRepository.findByMaVaiTro("HDTPOS_STAFF");
        if (hienTai != null) {
            return hienTai;
        }
        VaiTro vt = new VaiTro();
        vt.setMaVaiTro("HDTPOS_STAFF");
        vt.setTenVaiTro("Test Hoa Don POS Staff");
        vt.setTrangThai(true);
        VaiTro saved = vaiTroRepository.save(vt);
        idVaiTroTao = saved.getId();
        return saved;
    }

    private NhanVien taoNhanVien(String tienTo, VaiTro vaiTro) {
        String suffix = String.valueOf(System.nanoTime());
        NhanVien nhanVien = new NhanVien();
        nhanVien.setVaiTro(vaiTro);
        nhanVien.setMaNhanVien(tienTo + suffix);
        nhanVien.setTenTaiKhoan(tienTo + suffix);
        nhanVien.setMatKhau("x");
        nhanVien.setTrangThai(true);
        NhanVien saved = nhanVienRepository.save(nhanVien);
        idNVTao.add(saved.getId());
        return saved;
    }

    private PhuongThucThanhToan timPhuongThuc(String chua) {
        return phuongThucThanhToanRepository.findAll().stream()
            .filter(p -> p.getTenPhuongThuc().toLowerCase().contains(chua.toLowerCase()))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Thieu phuong thuc thanh toan: " + chua));
    }

    private static void dangNhap(Long idNhanVien) {
        var authorities = new ArrayList<SimpleGrantedAuthority>();
        authorities.add(new SimpleGrantedAuthority("ROLE_NHAN_VIEN"));
        SecurityContextHolder.getContext().setAuthentication(
            new TestingAuthenticationToken(idNhanVien, "n/a", authorities));
    }
}
