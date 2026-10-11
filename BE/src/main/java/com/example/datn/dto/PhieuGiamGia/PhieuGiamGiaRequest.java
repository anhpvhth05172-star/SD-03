package com.example.datn.dto.PhieuGiamGia;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
public class PhieuGiamGiaRequest {
    private String maPhieuGiamGia;
    private String tenPhieuGiamGia;
    private String loaiGiamGia;          // PERCENT | AMOUNT
    private BigDecimal giaTriGiam;
    private BigDecimal giamToiDa;
    private BigDecimal hoaDonToiThieu;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Integer soLuong;
    private Boolean trangThai;
    private String phamVi;               // CONG_KHAI | CA_NHAN
    private List<Long> danhSachKhachHangId;
    private String moTa;
}
