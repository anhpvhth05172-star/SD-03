package com.example.datn.service;

import com.example.datn.dto.ThongKe.ChiTietThongKeRow;
import com.example.datn.dto.ThongKe.DonHangThongKeDTO;
import com.example.datn.dto.ThongKe.HoaDonChiTietThongKeRow;
import com.example.datn.dto.ThongKe.HoaDonThongKeRow;
import com.example.datn.dto.ThongKe.KySoSanhDTO;
import com.example.datn.dto.ThongKe.SanPhamDaBanChiTietDTO;
import com.example.datn.dto.ThongKe.SanPhamDaBanChiTietRow;
import com.example.datn.dto.ThongKe.SanPhamTheoNgayDTO;
import com.example.datn.dto.ThongKe.SanPhamThongKeRow;
import com.example.datn.dto.ThongKe.SoSanhDTO;
import com.example.datn.dto.ThongKe.ThongKeSanPhamBangDTO;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.dto.ThongKe.TopSanPhamKyDTO;
import com.example.datn.dto.ThongKe.TrangThaiDonDTO;
import com.example.datn.dto.ThongKe.ThuongHieuThongKeResponse;
import com.example.datn.entity.HinhAnhSanPham;
import com.example.datn.entity.SanPham;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.repository.HinhAnhSanPhamRepository;
import com.example.datn.repository.SanPhamChiTietRepository;
import com.example.datn.repository.SanPhamRepository;
import com.example.datn.repository.ThongKeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class ThongKeService {

    private final ThongKeRepository thongKeRepository;
    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final HinhAnhSanPhamRepository hinhAnhSanPhamRepository;

    @Autowired
    public ThongKeService(
        ThongKeRepository thongKeRepository,
        SanPhamRepository sanPhamRepository,
        SanPhamChiTietRepository sanPhamChiTietRepository,
        HinhAnhSanPhamRepository hinhAnhSanPhamRepository
    ) {
        this.thongKeRepository = thongKeRepository;
        this.sanPhamRepository = sanPhamRepository;
        this.sanPhamChiTietRepository = sanPhamChiTietRepository;
        this.hinhAnhSanPhamRepository = hinhAnhSanPhamRepository;
    }

    public ThongKeService(ThongKeRepository thongKeRepository) {
        this(thongKeRepository, null, null, null);
    }

    private static final Set<String> TRANG_THAI_HUY_HOAN_TIEN = Set.of("DA_HUY", "DA_HOAN_TIEN");
    private static final Set<String> MA_HINH_THUC_TIEN_MAT = Set.of("TM", "CASH");

    private static boolean laHuyHoanTien(String trangThai) {
        return trangThai != null && TRANG_THAI_HUY_HOAN_TIEN.contains(trangThai);
    }

    private static boolean laTienMat(String maHinhThuc) {
        return maHinhThuc != null && MA_HINH_THUC_TIEN_MAT.contains(maHinhThuc);
    }

    private static long giaTriTien(BigDecimal tien) {
        return tien == null ? 0L : Math.max(0, tien.longValue());
    }

    private static BigDecimal giaTriBigDecimal(BigDecimal tien) {
        return tien == null ? BigDecimal.ZERO : tien;
    }

    private static final List<String> THU_TU_TRANG_THAI = List.of(
        "CHO_THANH_TOAN", "CHO_XAC_NHAN", "DA_XAC_NHAN", "CHO_GIAO_HANG", "DANG_GIAO_HANG",
        "GIAO_HANG_THAT_BAI", "DA_GIAO_HANG", "DA_HOAN_THANH", "DA_HUY", "DA_HOAN_TIEN"
    );

    private static final Map<String, String> NHAN_TRANG_THAI = Map.of(
        "CHO_THANH_TOAN", "Chờ thanh toán",
        "CHO_XAC_NHAN", "Chờ xác nhận",
        "DA_XAC_NHAN", "Đã xác nhận",
        "CHO_GIAO_HANG", "Chờ giao hàng",
        "DANG_GIAO_HANG", "Đang giao hàng",
        "DA_GIAO_HANG", "Đã giao hàng",
        "GIAO_HANG_THAT_BAI", "Giao hàng thất bại",
        "DA_HOAN_THANH", "Đã hoàn thành",
        "DA_HUY", "Đã hủy",
        "DA_HOAN_TIEN", "Đã hoàn tiền"
    );

    public TongQuanDTO tongQuan(LocalDate tu, LocalDate den, String thuongHieu) {
        LocalDateTime tuDT = tu.atStartOfDay();
        LocalDateTime denDT = den.plusDays(1).atStartOfDay();

        List<HoaDonThongKeRow> hoaDons = thongKeRepository.findHoaDonTrongKhoang(tuDT, denDT);
        List<ChiTietThongKeRow> chiTiets = thongKeRepository.findChiTietTrongKhoang(tuDT, denDT);
        long khachMoi = thongKeRepository.countKhachMoi(tuDT, denDT);

        if (thuongHieu == null || thuongHieu.isBlank()) {
            return tongQuanKhongLocThuongHieu(hoaDons, chiTiets, khachMoi);
        }
        return tongQuanLocThuongHieu(hoaDons, chiTiets, khachMoi, thuongHieu);
    }

    public static boolean daThanhToan(HoaDonThongKeRow h) {
        return daThanhToan(h.trangThai(), h.loaiDon(), h.maHinhThuc(), h.daXoa());
    }

    public static boolean daThanhToan(String tt, String loaiDon, String maHinhThuc, boolean daXoa) {
        if (daXoa || laHuyHoanTien(tt)) {
            return false;
        }
        if ("DA_HOAN_THANH".equals(tt) || "DA_GIAO_HANG".equals(tt)) {
            return true;
        }
        if ("ONLINE".equalsIgnoreCase(loaiDon)) {
            boolean laTienMatCod = laTienMat(maHinhThuc);
            if (!laTienMatCod && !"CHO_THANH_TOAN".equals(tt)) {
                return true;
            }
        }
        return false;
    }

    private TongQuanDTO tongQuanKhongLocThuongHieu(
        List<HoaDonThongKeRow> hoaDons,
        List<ChiTietThongKeRow> chiTiets,
        long khachMoi
    ) {
        Set<Long> idHopLe = hoaDons.stream()
            .filter(h -> !h.daXoa() && !laHuyHoanTien(h.trangThai()))
            .map(HoaDonThongKeRow::id)
            .collect(Collectors.toSet());

        Set<Long> idDaThanhToan = hoaDons.stream()
            .filter(ThongKeService::daThanhToan)
            .map(HoaDonThongKeRow::id)
            .collect(Collectors.toSet());

        long doanhThu = hoaDons.stream()
            .filter(h -> idDaThanhToan.contains(h.id()))
            .mapToLong(h -> giaTriTien(h.tienSauGiamGia()))
            .sum();

        long tienMat = hoaDons.stream()
            .filter(h -> idDaThanhToan.contains(h.id()))
            .filter(h -> laTienMat(h.maHinhThuc()))
            .mapToLong(h -> giaTriTien(h.tienSauGiamGia()))
            .sum();

        long tienChuyenKhoan = Math.max(0, doanhThu - tienMat);

        long doanhThuDuKien = hoaDons.stream()
            .filter(h -> idHopLe.contains(h.id()) && !idDaThanhToan.contains(h.id()))
            .mapToLong(h -> giaTriTien(h.tienSauGiamGia()))
            .sum();

        long soDon = idHopLe.size();
        long soDonDaTra = idDaThanhToan.size();
        long aov = soDonDaTra > 0 ? Math.round((double) doanhThu / soDonDaTra) : 0;
        long sanPhamBan = chiTiets.stream()
            .filter(c -> idHopLe.contains(c.idHoaDon()))
            .mapToLong(c -> c.soLuong() != null ? c.soLuong() : 0)
            .sum();

        long soDonOnline = demDonOnline(hoaDons, idHopLe);
        return new TongQuanDTO(doanhThu, tienMat, tienChuyenKhoan, soDon, aov, sanPhamBan, khachMoi,
            soDonOnline, soDon - soDonOnline, doanhThuDuKien);
    }

    /** Dem don ONLINE trong tap id; con lai (TAI_QUAY, TAI_QUAY_GIAO_HANG) la don tai quay. */
    private long demDonOnline(List<HoaDonThongKeRow> hoaDons, Set<Long> ids) {
        return hoaDons.stream()
            .filter(h -> ids.contains(h.id()) && "ONLINE".equalsIgnoreCase(h.loaiDon()))
            .count();
    }

    private TongQuanDTO tongQuanLocThuongHieu(
        List<HoaDonThongKeRow> hoaDons,
        List<ChiTietThongKeRow> chiTiets,
        long khachMoi,
        String thuongHieu
    ) {
        String thuongHieuLower = thuongHieu.toLowerCase();

        Set<Long> idHopLe = hoaDons.stream()
            .filter(h -> !h.daXoa() && !laHuyHoanTien(h.trangThai()))
            .map(HoaDonThongKeRow::id)
            .collect(Collectors.toSet());

        Set<Long> idDaThanhToan = hoaDons.stream()
            .filter(ThongKeService::daThanhToan)
            .map(HoaDonThongKeRow::id)
            .collect(Collectors.toSet());

        List<ChiTietThongKeRow> dongThuongHieu = chiTiets.stream()
            .filter(c -> idHopLe.contains(c.idHoaDon()))
            .filter(c -> c.tenThuongHieu() != null && c.tenThuongHieu().toLowerCase().equals(thuongHieuLower))
            .toList();

        long doanhThu = dongThuongHieu.stream()
            .filter(c -> idDaThanhToan.contains(c.idHoaDon()))
            .mapToLong(c -> giaTriTien(c.thanhTien()))
            .sum();

        long doanhThuDuKien = dongThuongHieu.stream()
            .filter(c -> !idDaThanhToan.contains(c.idHoaDon()))
            .mapToLong(c -> giaTriTien(c.thanhTien()))
            .sum();

        long sanPhamBan = dongThuongHieu.stream()
            .mapToLong(c -> c.soLuong() != null ? c.soLuong() : 0)
            .sum();

        Set<Long> idDonChuaThuongHieu = dongThuongHieu.stream()
            .map(ChiTietThongKeRow::idHoaDon)
            .collect(Collectors.toSet());
        long soDon = idDonChuaThuongHieu.size();

        long soDonDaTra = dongThuongHieu.stream()
            .filter(c -> idDaThanhToan.contains(c.idHoaDon()))
            .map(ChiTietThongKeRow::idHoaDon)
            .distinct()
            .count();

        Map<Long, BigDecimal> tongTienTungDon = new LinkedHashMap<>();
        for (ChiTietThongKeRow c : chiTiets) {
            if (idDaThanhToan.contains(c.idHoaDon())) {
                tongTienTungDon.merge(c.idHoaDon(), giaTriBigDecimal(c.thanhTien()), BigDecimal::add);
            }
        }

        Map<Long, BigDecimal> tienThuongHieuTungDon = new LinkedHashMap<>();
        for (ChiTietThongKeRow c : dongThuongHieu) {
            if (idDaThanhToan.contains(c.idHoaDon())) {
                tienThuongHieuTungDon.merge(c.idHoaDon(), giaTriBigDecimal(c.thanhTien()), BigDecimal::add);
            }
        }

        long tienMat = 0;
        for (HoaDonThongKeRow h : hoaDons) {
            if (!idDaThanhToan.contains(h.id()) || !laTienMat(h.maHinhThuc())) {
                continue;
            }
            BigDecimal tongTien = tongTienTungDon.get(h.id());
            BigDecimal tienThuongHieu = tienThuongHieuTungDon.get(h.id());
            if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) == 0 || tienThuongHieu == null) {
                continue;
            }
            BigDecimal tyTrong = tienThuongHieu.divide(tongTien, 10, RoundingMode.HALF_UP);
            tienMat += giaTriBigDecimal(h.tienSauGiamGia()).multiply(tyTrong).setScale(0, RoundingMode.HALF_UP).longValue();
        }

        long aov = soDonDaTra > 0 ? Math.round((double) doanhThu / soDonDaTra) : 0;
        long tienChuyenKhoan = Math.max(0, doanhThu - tienMat);

        long soDonOnline = demDonOnline(hoaDons, idDonChuaThuongHieu);
        return new TongQuanDTO(doanhThu, tienMat, tienChuyenKhoan, soDon, aov, sanPhamBan, khachMoi,
            soDonOnline, soDon - soDonOnline, doanhThuDuKien);
    }

    /**
     * Dem don da huy/hoan tien trong khoang ngay (loai don da xoa).
     */
    public long demDonHuy(LocalDate tu, LocalDate den) {
        LocalDateTime tuDT = tu.atStartOfDay();
        LocalDateTime denDT = den.plusDays(1).atStartOfDay();
        return thongKeRepository.findHoaDonTrongKhoang(tuDT, denDT).stream()
            .filter(h -> !h.daXoa() && laHuyHoanTien(h.trangThai()))
            .count();
    }

    public List<TrangThaiDonDTO> trangThaiDon(LocalDate tu, LocalDate den, String thuongHieu) {
        LocalDateTime tuDT = tu.atStartOfDay();
        LocalDateTime denDT = den.plusDays(1).atStartOfDay();

        List<HoaDonThongKeRow> hoaDons = thongKeRepository.findHoaDonTrongKhoang(tuDT, denDT);

        Map<String, Long> demTheoTrangThai = new LinkedHashMap<>();
        for (String ma : THU_TU_TRANG_THAI) {
            demTheoTrangThai.put(ma, 0L);
        }

        if (thuongHieu == null || thuongHieu.isBlank()) {
            for (HoaDonThongKeRow h : hoaDons) {
                if (!h.daXoa() && demTheoTrangThai.containsKey(h.trangThai())) {
                    demTheoTrangThai.merge(h.trangThai(), 1L, Long::sum);
                }
            }
        } else {
            String thuongHieuLower = thuongHieu.toLowerCase();
            List<ChiTietThongKeRow> chiTiets = thongKeRepository.findChiTietTrongKhoang(tuDT, denDT);

            Set<Long> idHopLe = hoaDons.stream()
                .filter(h -> !h.daXoa())
                .map(HoaDonThongKeRow::id)
                .collect(Collectors.toSet());

            Map<Long, String> trangThaiTheoId = new LinkedHashMap<>();
            for (HoaDonThongKeRow h : hoaDons) {
                if (h.id() != null && idHopLe.contains(h.id())) {
                    trangThaiTheoId.put(h.id(), h.trangThai());
                }
            }

            Set<Long> idDonChuaThuongHieu = chiTiets.stream()
                .filter(c -> idHopLe.contains(c.idHoaDon()))
                .filter(c -> c.tenThuongHieu() != null && c.tenThuongHieu().toLowerCase().equals(thuongHieuLower))
                .map(ChiTietThongKeRow::idHoaDon)
                .collect(Collectors.toSet());

            for (Long idDon : idDonChuaThuongHieu) {
                String trangThai = trangThaiTheoId.get(idDon);
                if (trangThai != null && demTheoTrangThai.containsKey(trangThai)) {
                    demTheoTrangThai.merge(trangThai, 1L, Long::sum);
                }
            }
        }

        return THU_TU_TRANG_THAI.stream()
            .map(ma -> new TrangThaiDonDTO(NHAN_TRANG_THAI.get(ma), demTheoTrangThai.get(ma)))
            .toList();
    }

    /* ---------------------------------- So sanh doanh thu ---------------------------------- */

    private record RanhKy(String maKy, String tenKy, LocalDate tu, LocalDate den) {}

    /**
     * Tinh cac ky so sanh theo che do thang/quy/nam/2ky.
     * Cong thuc doanh thu/AOV/tienMat lay dung tu tongQuanKhongLocThuongHieu/tongQuanLocThuongHieu.
     */
    public List<KySoSanhDTO> tinhKySoSanh(
        String cheDo, Integer nam, LocalDate tu1, LocalDate den1, LocalDate tu2, LocalDate den2, String thuongHieu
    ) {
        List<RanhKy> ds = danhSachKy(cheDo, nam, tu1, den1, tu2, den2);

        List<TongQuanDTO> ketQua = new ArrayList<>(ds.size());
        long max = 0;
        for (RanhKy k : ds) {
            LocalDateTime tuDT = k.tu().atStartOfDay();
            LocalDateTime denDT = k.den().plusDays(1).atStartOfDay();
            List<HoaDonThongKeRow> hoaDons = thongKeRepository.findHoaDonTrongKhoang(tuDT, denDT);
            List<ChiTietThongKeRow> chiTiets = thongKeRepository.findChiTietTrongKhoang(tuDT, denDT);
            TongQuanDTO t = (thuongHieu == null || thuongHieu.isBlank())
                ? tongQuanKhongLocThuongHieu(hoaDons, chiTiets, 0)
                : tongQuanLocThuongHieu(hoaDons, chiTiets, 0, thuongHieu);
            ketQua.add(t);
            max = Math.max(max, t.doanhThu());
        }

        List<KySoSanhDTO> rs = new ArrayList<>(ds.size());
        long prev = 0;
        for (int i = 0; i < ds.size(); i++) {
            RanhKy k = ds.get(i);
            TongQuanDTO t = ketQua.get(i);
            long chenhLech = t.doanhThu() - prev;
            int tangTruong;
            if (prev <= 0) {
                tangTruong = t.doanhThu() > 0 ? 100 : 0;
            } else {
                tangTruong = (int) Math.round((t.doanhThu() - prev) * 100.0 / prev);
            }
            rs.add(new KySoSanhDTO(
                k.maKy(), k.tenKy(), k.tu(), k.den(),
                t.doanhThu(), t.tienMat(), t.tienChuyenKhoan(), t.soDon(), t.aov(), t.sanPhamBan(),
                chenhLech, tangTruong, t.doanhThu() > 0 && t.doanhThu() == max
            ));
            prev = t.doanhThu();
        }
        return rs;
    }

    private List<RanhKy> danhSachKy(String cheDo, Integer nam, LocalDate tu1, LocalDate den1, LocalDate tu2, LocalDate den2) {
        if (cheDo == null || cheDo.isBlank()) {
            throw new IllegalArgumentException("Thiếu chế độ đối chiếu (cheDo)");
        }
        return switch (cheDo) {
            case "thang" -> {
                int n = kiemTraNam(nam);
                List<RanhKy> ds = new ArrayList<>(12);
                for (int m = 1; m <= 12; m++) {
                    LocalDate tu = LocalDate.of(n, m, 1);
                    LocalDate den = LocalDate.of(n, m, tu.lengthOfMonth());
                    ds.add(new RanhKy("T" + m, "Tháng " + m, tu, den));
                }
                yield ds;
            }
            case "quy" -> {
                int n = kiemTraNam(nam);
                List<RanhKy> ds = new ArrayList<>(4);
                for (int q = 1; q <= 4; q++) {
                    int thangDau = (q - 1) * 3 + 1;
                    int thangCuoi = thangDau + 2;
                    LocalDate tu = LocalDate.of(n, thangDau, 1);
                    LocalDate den = LocalDate.of(n, thangCuoi, LocalDate.of(n, thangCuoi, 1).lengthOfMonth());
                    ds.add(new RanhKy("Q" + q, "Quý " + q, tu, den));
                }
                yield ds;
            }
            case "nam" -> {
                int n = kiemTraNam(nam);
                List<Integer> namCoDuLieu = thongKeRepository.cacNamCoDuLieu();
                List<Integer> chon = (namCoDuLieu == null ? List.<Integer>of() : namCoDuLieu.stream()
                    .filter(y -> y != null && y <= n)
                    .limit(5)
                    .toList());
                if (chon.isEmpty()) {
                    chon = List.of(n);
                }
                List<RanhKy> ds = new ArrayList<>(chon.size());
                for (int y : chon) {
                    ds.add(new RanhKy(String.valueOf(y), String.valueOf(y), LocalDate.of(y, 1, 1), LocalDate.of(y, 12, 31)));
                }
                yield ds;
            }
            case "2ky" -> {
                if (tu1 == null || den1 == null || tu2 == null || den2 == null) {
                    throw new IllegalArgumentException("Chế độ 2 kỳ cần đủ 4 ngày từ ngày/đến ngày của cả 2 kỳ");
                }
                if (tu1.isAfter(den1)) {
                    throw new IllegalArgumentException("Kỳ 1: từ ngày phải trước hoặc bằng đến ngày");
                }
                if (tu2.isAfter(den2)) {
                    throw new IllegalArgumentException("Kỳ 2: từ ngày phải trước hoặc bằng đến ngày");
                }
                yield List.of(new RanhKy("K1", "Kỳ 1", tu1, den1), new RanhKy("K2", "Kỳ 2", tu2, den2));
            }
            default -> throw new IllegalArgumentException("cheDo không hợp lệ: " + cheDo + " (chấp nhận thang|quy|nam|2ky)");
        };
    }

    private int kiemTraNam(Integer nam) {
        int n = nam == null ? Year.now().getValue() : nam;
        if (n < 2000 || n > 2100) {
            throw new IllegalArgumentException("Năm không hợp lệ: " + n);
        }
        return n;
    }

    /**
     * So sanh doanh thu: cac ky doi chieu + top 10 san pham ban chay theo ky.
     * keyword chi loc bang top san pham (khong anh den so lieu ky).
     */
    public SoSanhDTO soSanh(
        String cheDo, Integer nam, LocalDate tu1, LocalDate den1, LocalDate tu2, LocalDate den2,
        String thuongHieu, String keyword
    ) {
        List<KySoSanhDTO> ky = tinhKySoSanh(cheDo, nam, tu1, den1, tu2, den2, thuongHieu);
        List<RanhKy> ds = danhSachKy(cheDo, nam, tu1, den1, tu2, den2);

        LocalDateTime tuAll = ds.stream().map(RanhKy::tu).min(LocalDate::compareTo).orElseThrow().atStartOfDay();
        LocalDateTime denAll = ds.stream().map(RanhKy::den).max(LocalDate::compareTo).orElseThrow().plusDays(1).atStartOfDay();
        List<SanPhamThongKeRow> rows = thongKeRepository.findSanPhamTrongKhoang(tuAll, denAll);

        String kw = keyword == null || keyword.isBlank() ? null : keyword.trim().toLowerCase();
        String th = thuongHieu == null || thuongHieu.isBlank() ? null : thuongHieu.trim().toLowerCase();

        Map<String, long[]> tongTheoSp = new LinkedHashMap<>();
        Map<String, long[]> banTheoKy = new LinkedHashMap<>();
        Map<String, SanPhamThongKeRow> mau = new LinkedHashMap<>();

        for (SanPhamThongKeRow r : rows) {
            if (r.ngayTao() == null || r.maSanPham() == null) {
                continue;
            }
            if (r.daXoa() || laHuyHoanTien(r.trangThai())) {
                continue;
            }
            if (th != null && (r.tenThuongHieu() == null || !r.tenThuongHieu().trim().toLowerCase().equals(th))) {
                continue;
            }
            if (kw != null) {
                String ma = r.maSanPham().toLowerCase();
                String ten = r.tenSanPham() == null ? "" : r.tenSanPham().toLowerCase();
                if (!ma.contains(kw) && !ten.contains(kw)) {
                    continue;
                }
            }
            int idx = -1;
            for (int i = 0; i < ds.size(); i++) {
                RanhKy k = ds.get(i);
                LocalDateTime tuK = k.tu().atStartOfDay();
                LocalDateTime denK = k.den().plusDays(1).atStartOfDay();
                if (!r.ngayTao().isBefore(tuK) && r.ngayTao().isBefore(denK)) {
                    idx = i;
                    break;
                }
            }
            if (idx < 0) {
                continue;
            }
            int sl = r.soLuong() == null ? 0 : r.soLuong();
            long tt = r.thanhTien() == null ? 0 : r.thanhTien().longValue();
            long[] t = tongTheoSp.computeIfAbsent(r.maSanPham(), x -> new long[2]);
            t[0] += sl;
            t[1] += tt;
            long[] b = banTheoKy.computeIfAbsent(r.maSanPham(), x -> new long[ds.size()]);
            b[idx] += sl;
            mau.putIfAbsent(r.maSanPham(), r);
        }

        List<String> maSapXep = new ArrayList<>(tongTheoSp.keySet());
        maSapXep.sort((a, b) -> {
            long[] ta = tongTheoSp.get(a);
            long[] tb = tongTheoSp.get(b);
            if (ta[0] != tb[0]) {
                return Long.compare(tb[0], ta[0]);
            }
            return Long.compare(tb[1], ta[1]);
        });

        List<TopSanPhamKyDTO> top = new ArrayList<>();
        for (String ma : maSapXep) {
            if (top.size() >= 10) {
                break;
            }
            SanPhamThongKeRow r = mau.get(ma);
            long[] t = tongTheoSp.get(ma);
            long[] b = banTheoKy.get(ma);
            List<Long> theoKy = new ArrayList<>(b.length);
            for (long v : b) {
                theoKy.add(v);
            }
            top.add(new TopSanPhamKyDTO(r.maSanPham(), r.tenSanPham(), r.tenThuongHieu(), t[0], t[1], theoKy));
        }

        return new SoSanhDTO(ky, top);
    }

    /* ---------------------------------- Thống kê theo ngày, thương hiệu, chi tiết đơn & bảng sản phẩm ---------------------------------- */

    public List<SanPhamTheoNgayDTO> sanPhamTheoNgay(LocalDate tu, LocalDate den, String thuongHieu) {
        LocalDateTime tuDT = tu.atStartOfDay();
        LocalDateTime denDT = den.plusDays(1).atStartOfDay();

        List<SanPhamThongKeRow> sanPhams = thongKeRepository.findSanPhamTrongKhoang(tuDT, denDT);
        List<HoaDonChiTietThongKeRow> hoaDons = thongKeRepository.findHoaDonChiTietTrongKhoang(tuDT, denDT);

        String th = (thuongHieu != null && !thuongHieu.isBlank()) ? thuongHieu.trim().toLowerCase() : null;

        Map<LocalDate, Long> soLuongTheoNgay = new LinkedHashMap<>();
        Map<LocalDate, Long> doanhThuTheoNgay = new LinkedHashMap<>();
        Map<LocalDate, Long> soDonTheoNgay = new LinkedHashMap<>();

        LocalDate cur = tu;
        while (!cur.isAfter(den)) {
            soLuongTheoNgay.put(cur, 0L);
            doanhThuTheoNgay.put(cur, 0L);
            soDonTheoNgay.put(cur, 0L);
            cur = cur.plusDays(1);
        }

        for (SanPhamThongKeRow sp : sanPhams) {
            if (sp.daXoa() || laHuyHoanTien(sp.trangThai()) || sp.ngayTao() == null) {
                continue;
            }
            if (th != null && (sp.tenThuongHieu() == null || !sp.tenThuongHieu().trim().toLowerCase().equals(th))) {
                continue;
            }
            LocalDate ngay = sp.ngayTao().toLocalDate();
            if (soLuongTheoNgay.containsKey(ngay)) {
                long sl = sp.soLuong() != null ? sp.soLuong() : 0;
                long tt = sp.thanhTien() != null ? sp.thanhTien().longValue() : 0;
                soLuongTheoNgay.merge(ngay, sl, Long::sum);
                doanhThuTheoNgay.merge(ngay, tt, Long::sum);
            }
        }

        for (HoaDonChiTietThongKeRow hd : hoaDons) {
            if (hd.daXoa() || laHuyHoanTien(hd.trangThai()) || hd.ngayTao() == null) {
                continue;
            }
            LocalDate ngay = hd.ngayTao().toLocalDate();
            if (soDonTheoNgay.containsKey(ngay)) {
                soDonTheoNgay.merge(ngay, 1L, Long::sum);
            }
        }

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM");
        List<SanPhamTheoNgayDTO> result = new ArrayList<>();
        cur = tu;
        while (!cur.isAfter(den)) {
            result.add(new SanPhamTheoNgayDTO(
                cur,
                cur.format(fmt),
                soLuongTheoNgay.getOrDefault(cur, 0L),
                doanhThuTheoNgay.getOrDefault(cur, 0L),
                soDonTheoNgay.getOrDefault(cur, 0L)
            ));
            cur = cur.plusDays(1);
        }
        return result;
    }

    public ThuongHieuThongKeResponse thuongHieuThongKe(LocalDate tu, LocalDate den) {
        LocalDateTime tuDT = tu.atStartOfDay();
        LocalDateTime denDT = den.plusDays(1).atStartOfDay();

        List<SanPhamThongKeRow> rows = thongKeRepository.findSanPhamTrongKhoang(tuDT, denDT);

        Map<String, long[]> thMap = new LinkedHashMap<>();
        long tongDt = 0;
        long tongSl = 0;

        for (SanPhamThongKeRow r : rows) {
            if (r.daXoa() || laHuyHoanTien(r.trangThai())) {
                continue;
            }
            String th = (r.tenThuongHieu() != null && !r.tenThuongHieu().isBlank()) ? r.tenThuongHieu().trim() : "Khác";
            long dt = r.thanhTien() != null ? r.thanhTien().longValue() : 0;
            long sl = r.soLuong() != null ? r.soLuong() : 0;

            long[] val = thMap.computeIfAbsent(th, k -> new long[2]);
            val[0] += dt;
            val[1] += sl;
            tongDt += dt;
            tongSl += sl;
        }

        String[] palette = { "#926c45", "#2563eb", "#16a34a", "#f59e0b", "#8b5cf6", "#ec4899", "#06b6d4" };
        List<ThuongHieuThongKeResponse.ThuongHieuItemDTO> list = new ArrayList<>();
        int colorIdx = 0;
        for (Map.Entry<String, long[]> e : thMap.entrySet()) {
            long dt = e.getValue()[0];
            long sl = e.getValue()[1];
            double tyLe = tongDt > 0 ? Math.round((dt * 100.0 / tongDt) * 10.0) / 10.0 : 0.0;
            String color = palette[colorIdx % palette.length];
            colorIdx++;
            list.add(new ThuongHieuThongKeResponse.ThuongHieuItemDTO(e.getKey(), dt, sl, tyLe, color));
        }

        list.sort((a, b) -> Long.compare(b.doanhThu(), a.doanhThu()));
        return new ThuongHieuThongKeResponse(tongDt, tongSl, list);
    }

    public List<ThongKeSanPhamBangDTO> sanPhamBang(LocalDate tu, LocalDate den, String thuongHieu, String keyword) {
        LocalDateTime tuDT = tu.atStartOfDay();
        LocalDateTime denDT = den.plusDays(1).atStartOfDay();

        List<SanPhamThongKeRow> rows = thongKeRepository.findSanPhamTrongKhoang(tuDT, denDT);

        Map<String, long[]> banTheoSp = new LinkedHashMap<>();
        for (SanPhamThongKeRow r : rows) {
            if (r.daXoa() || laHuyHoanTien(r.trangThai()) || r.maSanPham() == null) {
                continue;
            }
            long sl = r.soLuong() != null ? r.soLuong() : 0;
            long dt = r.thanhTien() != null ? r.thanhTien().longValue() : 0;
            long[] b = banTheoSp.computeIfAbsent(r.maSanPham(), k -> new long[2]);
            b[0] += sl;
            b[1] += dt;
        }

        List<SanPham> sanPhamList = sanPhamRepository != null ? sanPhamRepository.findAll() : List.of();
        List<SanPhamChiTiet> chiTietList = sanPhamChiTietRepository != null ? sanPhamChiTietRepository.findAllWithDetails() : List.of();

        Map<Long, Long> tonKhoMap = new LinkedHashMap<>();
        Map<Long, Set<String>> sizeMap = new LinkedHashMap<>();
        for (SanPhamChiTiet ct : chiTietList) {
            if (ct.getSanPham() == null || ct.getSanPham().getId() == null) continue;
            Long idSp = ct.getSanPham().getId();
            tonKhoMap.merge(idSp, ct.getSoLuong() != null ? ct.getSoLuong().longValue() : 0L, Long::sum);
            if (ct.getKichCo() != null && ct.getKichCo().getTenKichCo() != null) {
                sizeMap.computeIfAbsent(idSp, k -> new LinkedHashSet<>()).add(ct.getKichCo().getTenKichCo().trim());
            }
        }

        Map<Long, String> anhMap = new LinkedHashMap<>();
        if (hinhAnhSanPhamRepository != null) {
            List<HinhAnhSanPham> anhList = hinhAnhSanPhamRepository.findAll();
            for (HinhAnhSanPham h : anhList) {
                if (h.getSanPham() != null && h.getSanPham().getId() != null) {
                    anhMap.putIfAbsent(h.getSanPham().getId(), h.getDuongDan());
                }
            }
        }

        String th = (thuongHieu != null && !thuongHieu.isBlank()) ? thuongHieu.trim().toLowerCase() : null;
        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;

        List<ThongKeSanPhamBangDTO> result = new ArrayList<>();
        for (SanPham sp : sanPhamList) {
            String tenTH = sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : "Khác";
            if (th != null && !tenTH.trim().toLowerCase().equals(th)) {
                continue;
            }
            if (kw != null) {
                String ma = sp.getMaSanPham() != null ? sp.getMaSanPham().toLowerCase() : "";
                String ten = sp.getTenSanPham() != null ? sp.getTenSanPham().toLowerCase() : "";
                if (!ma.contains(kw) && !ten.contains(kw)) {
                    continue;
                }
            }

            long[] stats = banTheoSp.getOrDefault(sp.getMaSanPham(), new long[]{0, 0});
            long ton = tonKhoMap.getOrDefault(sp.getId(), 0L);
            Set<String> sizes = sizeMap.getOrDefault(sp.getId(), Set.of());
            String bienThe = sizes.isEmpty() ? "Tiêu chuẩn" : "Size: " + String.join(", ", sizes);
            String anh = anhMap.getOrDefault(sp.getId(), null);

            result.add(new ThongKeSanPhamBangDTO(
                sp.getId(),
                sp.getMaSanPham(),
                sp.getTenSanPham(),
                anh,
                bienThe,
                tenTH,
                stats[0],
                0L,
                stats[1],
                ton
            ));
        }

        result.sort((a, b) -> {
            if (b.daBan() != a.daBan()) return Long.compare(b.daBan(), a.daBan());
            return Long.compare(b.doanhThu(), a.doanhThu());
        });
        return result;
    }

    public List<DonHangThongKeDTO> donHangChiTiet(
        LocalDate tu, LocalDate den, String kpiType, String loaiDon, String hinhThucTT,
        String trangThai, String keyword, String thuongHieu
    ) {
        LocalDateTime tuDT = tu.atStartOfDay();
        LocalDateTime denDT = den.plusDays(1).atStartOfDay();

        List<HoaDonChiTietThongKeRow> rows = thongKeRepository.findHoaDonChiTietTrongKhoang(tuDT, denDT);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;
        String ld = (loaiDon != null && !loaiDon.isBlank() && !"ALL".equalsIgnoreCase(loaiDon)) ? loaiDon.trim().toUpperCase() : null;
        String ht = (hinhThucTT != null && !hinhThucTT.isBlank() && !"ALL".equalsIgnoreCase(hinhThucTT)) ? hinhThucTT.trim().toUpperCase() : null;
        String tt = (trangThai != null && !trangThai.isBlank() && !"ALL".equalsIgnoreCase(trangThai)) ? trangThai.trim() : null;

        List<DonHangThongKeDTO> result = new ArrayList<>();
        for (HoaDonChiTietThongKeRow r : rows) {
            if (r.daXoa()) {
                continue;
            }

            boolean daTT = daThanhToan(r.trangThai(), r.loaiDon(), r.maHinhThuc(), r.daXoa());
            boolean laHuy = laHuyHoanTien(r.trangThai());

            if ("DOANH_THU_THUC_TE".equalsIgnoreCase(kpiType) && !daTT) {
                continue;
            }
            if ("DOANH_THU_DU_KIEN".equalsIgnoreCase(kpiType) && (daTT || laHuy)) {
                continue;
            }
            if ("TIEN_MAT".equalsIgnoreCase(kpiType) && (!daTT || !laTienMat(r.maHinhThuc()))) {
                continue;
            }
            if ("CHUYEN_KHOAN".equalsIgnoreCase(kpiType) && (!daTT || laTienMat(r.maHinhThuc()))) {
                continue;
            }
            if ("ALL_VALID".equalsIgnoreCase(kpiType) && laHuy) {
                continue;
            }

            if (ld != null) {
                String rowLd = r.loaiDon() != null ? r.loaiDon().toUpperCase() : "";
                if ("TAI_QUAY".equals(ld) && !rowLd.contains("TAI_QUAY") && !rowLd.contains("TẠI QUẦY")) {
                    continue;
                }
                if ("ONLINE".equals(ld) && !rowLd.contains("ONLINE")) {
                    continue;
                }
            }

            if (ht != null) {
                boolean tm = laTienMat(r.maHinhThuc());
                if ("TIEN_MAT".equals(ht) && !tm) continue;
                if ("CHUYEN_KHOAN".equals(ht) && tm) continue;
            }

            if (tt != null && !tt.equalsIgnoreCase(r.trangThai()) && !tt.equalsIgnoreCase(NHAN_TRANG_THAI.get(r.trangThai()))) {
                continue;
            }

            if (kw != null) {
                String maHd = r.maHoaDon() != null ? r.maHoaDon().toLowerCase() : "";
                String tenKh = r.tenKhachHang() != null ? r.tenKhachHang().toLowerCase() : "";
                String sdt = r.soDienThoaiKhachHang() != null ? r.soDienThoaiKhachHang().toLowerCase() : "";
                if (!maHd.contains(kw) && !tenKh.contains(kw) && !sdt.contains(kw)) {
                    continue;
                }
            }

            String trangThaiThanhToan = daTT ? "Đã thanh toán" : "Chưa thanh toán";
            String tenNv = (r.tenNhanVien() != null && !r.tenNhanVien().isBlank()) ? r.tenNhanVien() : "Khách hàng online";
            String tenKh = (r.tenKhachHang() != null && !r.tenKhachHang().isBlank()) ? r.tenKhachHang() : "Khách lẻ";
            String hinhThucDisplay = laTienMat(r.maHinhThuc()) ? "Tiền mặt" : "Chuyển khoản";
            String trangThaiDisplay = NHAN_TRANG_THAI.getOrDefault(r.trangThai(), r.trangThai());

            result.add(new DonHangThongKeDTO(
                r.id(),
                r.maHoaDon(),
                r.ngayTao(),
                tenKh,
                r.soDienThoaiKhachHang(),
                tenNv,
                r.loaiDon(),
                hinhThucDisplay,
                trangThaiDisplay,
                trangThaiThanhToan,
                r.tienSauGiamGia()
            ));
        }

        return result;
    }

    public List<SanPhamDaBanChiTietDTO> sanPhamDaBanChiTiet(
        LocalDate tu, LocalDate den, String thuongHieu, String keyword
    ) {
        LocalDateTime tuDT = tu.atStartOfDay();
        LocalDateTime denDT = den.plusDays(1).atStartOfDay();

        List<SanPhamDaBanChiTietRow> rows = thongKeRepository.findSanPhamDaBanChiTietTrongKhoang(tuDT, denDT);

        String kw = (keyword != null && !keyword.isBlank()) ? keyword.trim().toLowerCase() : null;
        String th = (thuongHieu != null && !thuongHieu.isBlank()) ? thuongHieu.trim().toLowerCase() : null;

        List<SanPhamDaBanChiTietDTO> result = new ArrayList<>();
        for (SanPhamDaBanChiTietRow r : rows) {
            if (r.daXoa() || laHuyHoanTien(r.trangThai())) {
                continue;
            }
            if (th != null && (r.tenThuongHieu() == null || !r.tenThuongHieu().trim().toLowerCase().equals(th))) {
                continue;
            }
            if (kw != null) {
                String ma = r.maSanPham() != null ? r.maSanPham().toLowerCase() : "";
                String ten = r.tenSanPham() != null ? r.tenSanPham().toLowerCase() : "";
                String maHd = r.maHoaDon() != null ? r.maHoaDon().toLowerCase() : "";
                if (!ma.contains(kw) && !ten.contains(kw) && !maHd.contains(kw)) {
                    continue;
                }
            }
            result.add(new SanPhamDaBanChiTietDTO(
                r.idHoaDon(),
                r.maHoaDon(),
                r.ngayTao(),
                r.maSanPham(),
                r.tenSanPham(),
                r.bienThe(),
                r.tenThuongHieu(),
                r.soLuong(),
                r.donGia(),
                r.thanhTien()
            ));
        }
        return result;
    }
}
