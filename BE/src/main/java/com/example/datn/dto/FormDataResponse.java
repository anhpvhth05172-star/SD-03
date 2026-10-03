package com.example.datn.dto;

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

    public record KhachHangOption(Long id, String ten, String soDienThoai) {}

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
        Integer soLuongDaSuDung
    ) {}

    public record SanPhamChiTietOption(
        Long id,
        String ma,
        String tenSanPham,
        String tenKichCo,
        String tenMau,
        BigDecimal giaBan,
        Integer soLuong
    ) {}
}
