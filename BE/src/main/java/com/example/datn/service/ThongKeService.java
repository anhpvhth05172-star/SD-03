package com.example.datn.service;

import com.example.datn.dto.ThongKe.ChiTietThongKeRow;
import com.example.datn.dto.ThongKe.HoaDonThongKeRow;
import com.example.datn.dto.ThongKe.KySoSanhDTO;
import com.example.datn.dto.ThongKe.SanPhamThongKeRow;
import com.example.datn.dto.ThongKe.SoSanhDTO;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.dto.ThongKe.TopSanPhamKyDTO;
import com.example.datn.dto.ThongKe.TrangThaiDonDTO;
import com.example.datn.repository.ThongKeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Year;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ThongKeService {

    private final ThongKeRepository thongKeRepository;

    private static final Set<String> TRANG_THAI_HUY_HOAN_TIEN = Set.of("DA_HUY", "DA_HOAN_TIEN");
    private static final Set<String> MA_HINH_THUC_TIEN_MAT = Set.of("TM", "CASH");

    private static final List<String> THU_TU_TRANG_THAI = List.of(
        "CHO_XAC_NHAN", "DA_XAC_NHAN", "CHO_GIAO_HANG", "DANG_GIAO_HANG",
        "DA_GIAO_HANG", "DA_HOAN_THANH", "DA_HUY", "DA_HOAN_TIEN"
    );

    private static final Map<String, String> NHAN_TRANG_THAI = Map.of(
        "CHO_XAC_NHAN", "Chờ xác nhận",
        "DA_XAC_NHAN", "Đã xác nhận",
        "CHO_GIAO_HANG", "Chờ giao hàng",
        "DANG_GIAO_HANG", "Đang giao hàng",
        "DA_GIAO_HANG", "Đã giao hàng",
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

    private TongQuanDTO tongQuanKhongLocThuongHieu(
        List<HoaDonThongKeRow> hoaDons,
        List<ChiTietThongKeRow> chiTiets,
        long khachMoi
    ) {
        Set<Long> idHopLe = hoaDons.stream()
            .filter(h -> !h.daXoa() && !TRANG_THAI_HUY_HOAN_TIEN.contains(h.trangThai()))
            .map(HoaDonThongKeRow::id)
            .collect(Collectors.toSet());

        long doanhThu = hoaDons.stream()
            .filter(h -> idHopLe.contains(h.id()))
            .mapToLong(h -> h.tienSauGiamGia().longValue())
            .sum();

        long tienMat = hoaDons.stream()
            .filter(h -> idHopLe.contains(h.id()))
            .filter(h -> MA_HINH_THUC_TIEN_MAT.contains(h.maHinhThuc()))
            .mapToLong(h -> h.tienSauGiamGia().longValue())
            .sum();

        long soDon = idHopLe.size();
        long aov = soDon > 0 ? Math.round((double) doanhThu / soDon) : 0;
        long tienChuyenKhoan = doanhThu - tienMat;
        long sanPhamBan = chiTiets.stream()
            .filter(c -> idHopLe.contains(c.idHoaDon()))
            .mapToLong(c -> c.soLuong() != null ? c.soLuong() : 0)
            .sum();

        return new TongQuanDTO(doanhThu, tienMat, tienChuyenKhoan, soDon, aov, sanPhamBan, khachMoi);
    }

    private TongQuanDTO tongQuanLocThuongHieu(
        List<HoaDonThongKeRow> hoaDons,
        List<ChiTietThongKeRow> chiTiets,
        long khachMoi,
        String thuongHieu
    ) {
        String thuongHieuLower = thuongHieu.toLowerCase();

        Set<Long> idHopLe = hoaDons.stream()
            .filter(h -> !h.daXoa() && !TRANG_THAI_HUY_HOAN_TIEN.contains(h.trangThai()))
            .map(HoaDonThongKeRow::id)
            .collect(Collectors.toSet());

        List<ChiTietThongKeRow> dongThuongHieu = chiTiets.stream()
            .filter(c -> idHopLe.contains(c.idHoaDon()))
            .filter(c -> c.tenThuongHieu() != null && c.tenThuongHieu().toLowerCase().equals(thuongHieuLower))
            .toList();

        long doanhThu = dongThuongHieu.stream()
            .mapToLong(c -> c.thanhTien().longValue())
            .sum();

        long sanPhamBan = dongThuongHieu.stream()
            .mapToLong(c -> c.soLuong() != null ? c.soLuong() : 0)
            .sum();

        Set<Long> idDonChuaThuongHieu = dongThuongHieu.stream()
            .map(ChiTietThongKeRow::idHoaDon)
            .collect(Collectors.toSet());
        long soDon = idDonChuaThuongHieu.size();

        Map<Long, BigDecimal> tongTienTungDon = new LinkedHashMap<>();
        for (ChiTietThongKeRow c : chiTiets) {
            if (idHopLe.contains(c.idHoaDon())) {
                tongTienTungDon.merge(c.idHoaDon(), c.thanhTien(), BigDecimal::add);
            }
        }

        Map<Long, BigDecimal> tienThuongHieuTungDon = new LinkedHashMap<>();
        for (ChiTietThongKeRow c : dongThuongHieu) {
            tienThuongHieuTungDon.merge(c.idHoaDon(), c.thanhTien(), BigDecimal::add);
        }

        long tienMat = 0;
        for (HoaDonThongKeRow h : hoaDons) {
            if (!idHopLe.contains(h.id()) || !MA_HINH_THUC_TIEN_MAT.contains(h.maHinhThuc())) {
                continue;
            }
            BigDecimal tongTien = tongTienTungDon.get(h.id());
            BigDecimal tienThuongHieu = tienThuongHieuTungDon.get(h.id());
            if (tongTien == null || tongTien.compareTo(BigDecimal.ZERO) == 0 || tienThuongHieu == null) {
                continue;
            }
            BigDecimal tyTrong = tienThuongHieu.divide(tongTien, 10, RoundingMode.HALF_UP);
            tienMat += h.tienSauGiamGia().multiply(tyTrong).setScale(0, RoundingMode.HALF_UP).longValue();
        }

        long aov = soDon > 0 ? Math.round((double) doanhThu / soDon) : 0;
        long tienChuyenKhoan = doanhThu - tienMat;

        return new TongQuanDTO(doanhThu, tienMat, tienChuyenKhoan, soDon, aov, sanPhamBan, khachMoi);
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

            Map<Long, String> trangThaiTheoId = hoaDons.stream()
                .filter(h -> idHopLe.contains(h.id()))
                .collect(Collectors.toMap(HoaDonThongKeRow::id, HoaDonThongKeRow::trangThai));

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
            if (r.daXoa() || TRANG_THAI_HUY_HOAN_TIEN.contains(r.trangThai())) {
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
}
