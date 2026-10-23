package com.example.datn.service;

import com.example.datn.dto.ThongKe.ChiTietThongKeRow;
import com.example.datn.dto.ThongKe.HoaDonThongKeRow;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.dto.ThongKe.TrangThaiDonDTO;
import com.example.datn.repository.ThongKeRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
}
