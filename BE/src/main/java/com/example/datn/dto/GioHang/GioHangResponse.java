package com.example.datn.dto.GioHang;

import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;
@Getter
@Builder
public class GioHangResponse {

    private Integer idGioHang;
    private List<GioHangItemResponse> items;
    private Integer tongSoLuong;
    private BigDecimal tongTien;        // chỉ tính các dòng còn hàng
    private boolean coCanhBao;
}
