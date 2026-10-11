package com.example.datn.dto.DotGiamGia;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
@Getter
@Setter
public class DotGiamGiaRequest {
    private String maDotGiamGia;
    private String tenDotGiamGia;
    private BigDecimal phanTramGiam;
    private LocalDateTime ngayBatDau;
    private LocalDateTime ngayKetThuc;
    private Boolean trangThai;
    private String moTa;
    private List<Long> danhSachSanPhamChiTietId;
}
