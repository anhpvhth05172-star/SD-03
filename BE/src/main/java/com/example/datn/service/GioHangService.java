package com.example.datn.service;

import com.example.datn.dto.DotGiamGia.GiaKhuyenMai;
import com.example.datn.dto.GioHang.*;
import com.example.datn.entity.*;
import com.example.datn.repository.*;
import com.example.datn.service.impls.GiaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class GioHangService {

    private static final String TRANG_THAI_GIO_HANG = "HOAT_DONG"; // đổi theo dữ liệu thực tế của bạn
    private static final int MAX_SO_LUONG_MOI_DONG = 100;

    private final GioHangRepository gioHangRepository;
    private final GioHangChiTietRepository gioHangChiTietRepository;
    private final KhachHangRepository khachHangRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final HinhAnhSanPhamRepository hinhAnhSanPhamRepository;
    private final GiaService giaService;

    @Transactional
    public AddGioHangResponse themVaoGioHang(ThemGioHangRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu không hợp lệ");
        }
        if (req.getIdKhachHang() == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để thêm vào giỏ hàng");
        }
        if (req.getIdSanPhamChiTiet() == null) {
            throw new IllegalArgumentException("Vui lòng chọn biến thể sản phẩm (màu, kích cỡ)");
        }
        if (req.getSoLuong() == null || req.getSoLuong() < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn hoặc bằng 1");
        }
        if (req.getSoLuong() > MAX_SO_LUONG_MOI_DONG) {
            throw new IllegalArgumentException("Số lượng mỗi lần thêm tối đa là " + MAX_SO_LUONG_MOI_DONG);
        }

        KhachHang khachHang = layKhachHang(req.getIdKhachHang());

        // Khóa dòng biến thể để kiểm tra tồn kho an toàn khi nhiều người thao tác cùng lúc
        SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdForUpdate(req.getIdSanPhamChiTiet())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể sản phẩm với ID: " + req.getIdSanPhamChiTiet()));

        kiemTraBanDuoc(spct);
        int tonKho = tonKhoCua(spct);
        int donGia = tinhDonGia(spct);

        GioHang gioHang = gioHangRepository
                .findFirstByKhachHangIdAndTrangThaiOrderByIdDesc(khachHang.getId(), TRANG_THAI_GIO_HANG)
                .orElseGet(() -> taoGioHangMoi(khachHang));

        GioHangChiTiet item = gioHangChiTietRepository
                .findByGioHangIdAndSanPhamChiTietId(gioHang.getId(), spct.getId())
                .orElse(null);

        int soLuongTrongGio = (item != null && item.getSoLuong() != null) ? item.getSoLuong() : 0;
        int soLuongMoi = soLuongTrongGio + req.getSoLuong();

        if (soLuongMoi > MAX_SO_LUONG_MOI_DONG) {
            throw new IllegalArgumentException("Mỗi sản phẩm chỉ được mua tối đa " + MAX_SO_LUONG_MOI_DONG
                    + " trong một giỏ hàng (hiện có " + soLuongTrongGio + ")");
        }
        if (soLuongMoi > tonKho) {
            throw new IllegalArgumentException(thongBaoVuotTonKho(soLuongTrongGio, tonKho));
        }

        LocalDateTime now = LocalDateTime.now();
        if (item == null) {
            item = new GioHangChiTiet();
            item.setGioHang(gioHang);
            item.setSanPhamChiTiet(spct);
            item.setNgayThem(now);
        }
        item.setSoLuong(soLuongMoi);
        item.setDonGia(donGia);
        GioHangChiTiet saved = gioHangChiTietRepository.save(item);

        gioHang.setNgayCapNhat(now);
        gioHangRepository.save(gioHang);

        SanPham sanPham = spct.getSanPham();
        return AddGioHangResponse.builder()
                .idGioHang(gioHang.getId())
                .idGioHangChiTiet(saved.getId())
                .idSanPhamChiTiet(spct.getId())
                .tenSanPham(sanPham.getTenSanPham())
                .mauSac(spct.getMauSac() != null ? spct.getMauSac().getTenMau() : null)
                .kichCo(spct.getKichCo() != null ? spct.getKichCo().getTenKichCo() : null)
                .soLuong(soLuongMoi)
                .donGia(donGia)
                .thanhTien(BigDecimal.valueOf(donGia).multiply(BigDecimal.valueOf(soLuongMoi)))
                .tongSoLuongGioHang(gioHangChiTietRepository.tongSoLuongTheoGioHang(gioHang.getId()))
                .message("Đã thêm sản phẩm vào giỏ hàng")
                .build();
    }

    // ============================== XEM GIỎ ==============================
    @Transactional(readOnly = true)
    public GioHangResponse xemGioHang(Long idKhachHang) {
        layKhachHang(idKhachHang);
        GioHang gioHang = gioHangRepository
                .findFirstByKhachHangIdAndTrangThaiOrderByIdDesc(idKhachHang, TRANG_THAI_GIO_HANG)
                .orElse(null);
        return buildGioHangResponse(gioHang);
    }

    @Transactional(readOnly = true)
    public int demSoLuong(Long idKhachHang) {
        if (idKhachHang == null) return 0;
        return gioHangRepository
                .findFirstByKhachHangIdAndTrangThaiOrderByIdDesc(idKhachHang, TRANG_THAI_GIO_HANG)
                .map(g -> gioHangChiTietRepository.tongSoLuongTheoGioHang(g.getId()))
                .orElse(0);
    }

    // ============================== SỬA SỐ LƯỢNG ==============================
    @Transactional
    public GioHangResponse capNhatSoLuong(Integer idGioHangChiTiet, UpdateGioHangRequest req) {
        if (idGioHangChiTiet == null) {
            throw new IllegalArgumentException("Thiếu mã dòng giỏ hàng");
        }
        if (req == null || req.getIdKhachHang() == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập");
        }
        if (req.getSoLuong() == null || req.getSoLuong() < 1) {
            throw new IllegalArgumentException("Số lượng phải lớn hơn hoặc bằng 1");
        }
        if (req.getSoLuong() > MAX_SO_LUONG_MOI_DONG) {
            throw new IllegalArgumentException("Số lượng tối đa là " + MAX_SO_LUONG_MOI_DONG);
        }

        GioHangChiTiet item = gioHangChiTietRepository
                .findByIdAndGioHangKhachHangId(idGioHangChiTiet, req.getIdKhachHang())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng của bạn"));

        SanPhamChiTiet spct = sanPhamChiTietRepository.findByIdForUpdate(item.getSanPhamChiTiet().getId())
                .orElseThrow(() -> new IllegalArgumentException("Sản phẩm không còn tồn tại"));

        kiemTraBanDuoc(spct);
        int tonKho = tonKhoCua(spct);
        if (req.getSoLuong() > tonKho) {
            throw new IllegalArgumentException("Kho chỉ còn " + tonKho + " sản phẩm");
        }

        item.setSoLuong(req.getSoLuong());
        item.setDonGia(tinhDonGia(spct));
        gioHangChiTietRepository.save(item);

        GioHang gioHang = item.getGioHang();
        gioHang.setNgayCapNhat(LocalDateTime.now());
        gioHangRepository.save(gioHang);

        return buildGioHangResponse(gioHang);
    }

    // ============================== XÓA ==============================
    @Transactional
    public GioHangResponse xoaSanPham(Integer idGioHangChiTiet, Long idKhachHang) {
        if (idGioHangChiTiet == null || idKhachHang == null) {
            throw new IllegalArgumentException("Dữ liệu yêu cầu không hợp lệ");
        }
        GioHangChiTiet item = gioHangChiTietRepository
                .findByIdAndGioHangKhachHangId(idGioHangChiTiet, idKhachHang)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm trong giỏ hàng của bạn"));

        GioHang gioHang = item.getGioHang();
        gioHangChiTietRepository.delete(item);
        gioHangChiTietRepository.flush();

        gioHang.setNgayCapNhat(LocalDateTime.now());
        gioHangRepository.save(gioHang);
        return buildGioHangResponse(gioHang);
    }

    @Transactional
    public GioHangResponse xoaTatCa(Long idKhachHang) {
        layKhachHang(idKhachHang);
        GioHang gioHang = gioHangRepository
                .findFirstByKhachHangIdAndTrangThaiOrderByIdDesc(idKhachHang, TRANG_THAI_GIO_HANG)
                .orElse(null);
        if (gioHang != null) {
            gioHangChiTietRepository.xoaTatCaTheoGioHang(gioHang.getId());
            gioHang.setNgayCapNhat(LocalDateTime.now());
            gioHangRepository.save(gioHang);
        }
        return buildGioHangResponse(gioHang);
    }

    // ============================== HÀM PHỤ ==============================
    private KhachHang layKhachHang(Long idKhachHang) {
        if (idKhachHang == null) {
            throw new IllegalArgumentException("Vui lòng đăng nhập để sử dụng giỏ hàng");
        }
        return khachHangRepository.findById(idKhachHang)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy khách hàng với ID: " + idKhachHang));
    }

    private void kiemTraBanDuoc(SanPhamChiTiet spct) {
        SanPham sanPham = spct.getSanPham();
        if (sanPham == null) {
            throw new IllegalArgumentException("Biến thể sản phẩm không thuộc sản phẩm nào");
        }
        if (Boolean.FALSE.equals(sanPham.getTrangThai())) {
            throw new IllegalArgumentException("Sản phẩm '" + sanPham.getTenSanPham() + "' đã ngừng kinh doanh");
        }
        if (Boolean.FALSE.equals(spct.getTrangThai())) {
            throw new IllegalArgumentException("Biến thể này đã ngừng bán");
        }
        if (tonKhoCua(spct) <= 0) {
            throw new IllegalArgumentException("Sản phẩm đã hết hàng");
        }
    }

    private int tonKhoCua(SanPhamChiTiet spct) {
        return spct.getSoLuong() != null ? spct.getSoLuong() : 0;
    }

    private String thongBaoVuotTonKho(int soLuongTrongGio, int tonKho) {
        if (soLuongTrongGio > 0) {
            return "Giỏ hàng đã có " + soLuongTrongGio + " sản phẩm này, kho chỉ còn " + tonKho
                    + ". Bạn chỉ có thể thêm tối đa " + Math.max(tonKho - soLuongTrongGio, 0) + " sản phẩm nữa";
        }
        return "Kho chỉ còn " + tonKho + " sản phẩm, không đủ số lượng bạn yêu cầu";
    }

    private GioHang taoGioHangMoi(KhachHang khachHang) {
        LocalDateTime now = LocalDateTime.now();
        GioHang gh = new GioHang();
        gh.setKhachHang(khachHang);
        gh.setTrangThai(TRANG_THAI_GIO_HANG);
        gh.setNgayTao(now);
        gh.setNgayCapNhat(now);
        gh = gioHangRepository.save(gh);
        gh.setMaGioHang(String.format("GH%05d", gh.getId()));
        return gioHangRepository.save(gh);
    }

    /** Giá sau giảm (nếu có đợt giảm giá), không thì giá bán gốc. */
    private BigDecimal giaThucTe(SanPhamChiTiet spct, Map<Long, GiaKhuyenMai> giaMap) {
        GiaKhuyenMai gia = giaMap != null ? giaMap.get(spct.getId()) : null;
        BigDecimal giaThuc = (gia != null && gia.giaSauGiam() != null) ? gia.giaSauGiam() : spct.getGiaBan();
        if (giaThuc == null || giaThuc.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Sản phẩm chưa có giá bán hợp lệ");
        }
        return giaThuc;
    }

    private int tinhDonGia(SanPhamChiTiet spct) {
        BigDecimal giaThuc = giaThucTe(spct, giaService.tinhGia(List.of(spct)));
        try {
            return giaThuc.setScale(0, RoundingMode.HALF_UP).intValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("Giá sản phẩm vượt quá giới hạn cho phép");
        }
    }

    private GioHangResponse buildGioHangResponse(GioHang gioHang) {
        if (gioHang == null) {
            return GioHangResponse.builder()
                    .items(List.of()).tongSoLuong(0).tongTien(BigDecimal.ZERO).coCanhBao(false).build();
        }

        List<GioHangChiTiet> dong = gioHangChiTietRepository.findByGioHangIdOrderByNgayThemDesc(gioHang.getId());
        List<SanPhamChiTiet> spcts = dong.stream().map(GioHangChiTiet::getSanPhamChiTiet).toList();
        // Tính giá khuyến mãi 1 lần cho cả giỏ (tránh N+1)
        Map<Long, GiaKhuyenMai> giaMap = spcts.isEmpty() ? Map.of() : giaService.tinhGia(spcts);

        List<GioHangItemResponse> items = new ArrayList<>();
        BigDecimal tongTien = BigDecimal.ZERO;
        int tongSoLuong = 0;
        boolean coCanhBao = false;

        for (GioHangChiTiet d : dong) {
            SanPhamChiTiet spct = d.getSanPhamChiTiet();
            SanPham sp = spct.getSanPham();
            int soLuong = d.getSoLuong() != null ? d.getSoLuong() : 0;
            int tonKho = tonKhoCua(spct);

            boolean ngungBan = Boolean.FALSE.equals(spct.getTrangThai())
                    || (sp != null && Boolean.FALSE.equals(sp.getTrangThai()));
            String canhBao = null;
            boolean conHang = true;
            if (ngungBan) {
                canhBao = "Sản phẩm đã ngừng bán";
                conHang = false;
            } else if (tonKho <= 0) {
                canhBao = "Sản phẩm đã hết hàng";
                conHang = false;
            } else if (soLuong > tonKho) {
                canhBao = "Kho chỉ còn " + tonKho + " sản phẩm, vui lòng giảm số lượng";
            }

            BigDecimal giaHienTai;
            try {
                giaHienTai = giaThucTe(spct, giaMap);
            } catch (IllegalArgumentException ex) {
                giaHienTai = BigDecimal.ZERO;
                conHang = false;
                canhBao = "Sản phẩm chưa có giá bán";
            }

            if (canhBao == null && d.getDonGia() != null
                    && giaHienTai.compareTo(BigDecimal.valueOf(d.getDonGia())) != 0) {
                canhBao = "Giá đã thay đổi so với lúc bạn thêm vào giỏ";
            }

            BigDecimal thanhTien = giaHienTai.multiply(BigDecimal.valueOf(soLuong));
            if (conHang) {
                tongTien = tongTien.add(thanhTien);
                tongSoLuong += soLuong;
            }
            if (canhBao != null) coCanhBao = true;

            items.add(GioHangItemResponse.builder()
                    .id(d.getId())
                    .idSanPhamChiTiet(spct.getId())
                    .idSanPham(sp != null ? sp.getId() : null)
                    .tenSanPham(sp != null ? sp.getTenSanPham() : null)
                    .maCtsp(spct.getMaChiTietSanPham())
                    .mauSac(spct.getMauSac() != null ? spct.getMauSac().getTenMau() : null)
                    .kichCo(spct.getKichCo() != null ? spct.getKichCo().getTenKichCo() : null)
                    .hinhAnh(layHinhAnh(spct))
                    .soLuong(soLuong)
                    .donGia(d.getDonGia())
                    .giaHienTai(giaHienTai)
                    .thanhTien(thanhTien)
                    .tonKho(tonKho)
                    .conHang(conHang)
                    .canhBao(canhBao)
                    .build());
        }

        return GioHangResponse.builder()
                .idGioHang(gioHang.getId())
                .items(items)
                .tongSoLuong(tongSoLuong)
                .tongTien(tongTien)
                .coCanhBao(coCanhBao)
                .build();
    }

    /** Ảnh theo màu của biến thể (cùng quy ước COLOR_{idMau} như SanPhamChiTietServiceImpl), không có thì lấy ảnh đầu tiên. */
    private String layHinhAnh(SanPhamChiTiet spct) {
        if (spct.getSanPham() == null) return null;
        List<HinhAnhSanPham> images = hinhAnhSanPhamRepository.findBySanPhamId(spct.getSanPham().getId());
        if (images == null || images.isEmpty()) return null;

        if (spct.getMauSac() != null) {
            Long colorId = spct.getMauSac().getId();
            String cName = spct.getMauSac().getTenMau();
            String found = images.stream()
                    .filter(img -> img.getTenAnh() != null && (
                            (colorId != null && img.getTenAnh().contains("COLOR_" + colorId)) ||
                                    (cName != null && !cName.isBlank() && img.getTenAnh().toLowerCase().contains(cName.toLowerCase()))))
                    .map(HinhAnhSanPham::getDuongDan)
                    .findFirst().orElse(null);
            if (found != null) return found;
        }
        return images.get(0).getDuongDan();
    }
}
