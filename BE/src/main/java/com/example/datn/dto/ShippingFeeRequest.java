package com.example.datn.dto;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ShippingFeeRequest {

    private Integer toDistrictId;
    private String toWardCode;
    private List<DongHang> items;

    public record DongHang(Long idSanPhamChiTiet, Integer soLuong) {}
}
