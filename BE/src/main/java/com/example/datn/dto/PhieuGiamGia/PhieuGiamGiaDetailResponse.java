package com.example.datn.dto.PhieuGiamGia;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class PhieuGiamGiaDetailResponse {
    private Long id;
    private String maPhieuGiamGia;
    private String tenPhieuGiamGia;
    private String loaiGiamGia;
    private BigDecimal giaTriGiam;
    private BigDecimal giamToiDa;
    private BigDecimal hoaDonToiThieu;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Integer soLuong;
    private Integer soLuongDaSuDung;
    private Integer gioiHanMoiTaiKhoan;
    private Boolean trangThai;
    private String phamVi;
    private LocalDateTime ngayTao;
    private String moTa;
    private List<KhachHangItem> khachHangs;

    @Getter
    @Builder
    public static class KhachHangItem {
        private Long idKhachHang;
        private String maKhachHang;
        private String tenKhachHang;
        private String email;
        private String soDienThoai;
        private LocalDateTime ngaySuDung;
        private Boolean trangThai;
    }
}