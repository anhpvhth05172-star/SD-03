package com.example.datn.dto.GioHang;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
@Getter
@Builder
public class GioHangItemResponse {

    private Integer id;                 // id GioHangChiTiet
    private Long idSanPhamChiTiet;
    private Long idSanPham;
    private String tenSanPham;
    private String maCtsp;
    private String mauSac;
    private String kichCo;
    private String hinhAnh;
    private Integer soLuong;
    private Integer donGia;             // giá lúc thêm vào giỏ
    private BigDecimal giaHienTai;      // giá thực tế hiện tại (đã tính khuyến mãi)
    private BigDecimal thanhTien;       // giaHienTai * soLuong
    private Integer tonKho;
    private boolean conHang;            // false nếu hết hàng / ngừng bán
    private String canhBao;
}
