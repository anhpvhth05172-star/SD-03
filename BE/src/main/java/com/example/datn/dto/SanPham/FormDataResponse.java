package com.example.datn.dto.SanPham;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FormDataResponse {

    private List<KhachHangOption> khachHangs;
    private List<NhanVienOption> nhanViens;
    private List<PhuongThucOption> phuongThucThanhToans;
    private List<PhieuGiamGiaOption> phieuGiamGias;
    private List<SanPhamChiTietOption> sanPhamChiTiets;

    public record KhachHangOption(Long id, String ten, String soDienThoai, Boolean trangThai) {}

    public record NhanVienOption(Long id, String ten) {}

    public record PhuongThucOption(Long id, String ten) {}

    public record PhieuGiamGiaOption(
        Long id,
        String ma,
        String ten,
        String loaiGiamGia,
        BigDecimal giaTriGiam,
        BigDecimal giamToiDa,
        BigDecimal hoaDonToiThieu,
        LocalDateTime ngayBatDau,
        LocalDateTime ngayKetThuc,
        Integer soLuong,
        Integer soLuongDaSuDung,
        Integer gioiHanMoiTaiKhoan,
        Boolean trangThai,
        String phamVi,
        List<Long> idKhachHangsChuaDung,
        List<Long> idKhachHangsDaDung
    ) {
        public PhieuGiamGiaOption(
            Long id,
            String ma,
            String ten,
            String loaiGiamGia,
            BigDecimal giaTriGiam,
            BigDecimal giamToiDa,
            BigDecimal hoaDonToiThieu,
            LocalDateTime ngayBatDau,
            LocalDateTime ngayKetThuc,
            Integer soLuong,
            Integer soLuongDaSuDung,
            Integer gioiHanMoiTaiKhoan,
            Boolean trangThai
        ) {
            this(id, ma, ten, loaiGiamGia, giaTriGiam, giamToiDa, hoaDonToiThieu, ngayBatDau, ngayKetThuc, soLuong, soLuongDaSuDung, gioiHanMoiTaiKhoan, trangThai, "CONG_KHAI", List.of(), List.of());
        }

        public PhieuGiamGiaOption(
            Long id,
            String ma,
            String ten,
            String loaiGiamGia,
            BigDecimal giaTriGiam,
            BigDecimal giamToiDa,
            BigDecimal hoaDonToiThieu,
            LocalDateTime ngayBatDau,
            LocalDateTime ngayKetThuc,
            Integer soLuong,
            Integer soLuongDaSuDung,
            Integer gioiHanMoiTaiKhoan,
            Boolean trangThai,
            String phamVi,
            List<Long> idKhachHangsChuaDung
        ) {
            this(id, ma, ten, loaiGiamGia, giaTriGiam, giamToiDa, hoaDonToiThieu, ngayBatDau, ngayKetThuc, soLuong, soLuongDaSuDung, gioiHanMoiTaiKhoan, trangThai, phamVi, idKhachHangsChuaDung, List.of());
        }
    }

    public record SanPhamChiTietOption(
        Long id,
        String ma,
        String tenSanPham,
        String tenKichCo,
        String tenMau,
        BigDecimal giaBan,
        Integer soLuong,
        Boolean trangThai,
        Boolean trangThaiSanPham
    ) {}
}
