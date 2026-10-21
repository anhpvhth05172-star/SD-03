package com.example.datn.dto;

import java.util.List;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShippingFeeRequest {

    private Integer toDistrictId;
    private String toWardCode;
    private List<DongHang> items;
    private Integer serviceTypeId;
    private String toProvinceName;
    private String toWardName;

    public ShippingFeeRequest(Integer toDistrictId, String toWardCode, List<DongHang> items) {
        this.toDistrictId = toDistrictId;
        this.toWardCode = toWardCode;
        this.items = items;
    }

    public boolean coDiaChiMoi() {
        return (toDistrictId == null || isBlank(toWardCode))
            && !isBlank(toProvinceName)
            && !isBlank(toWardName);
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    public record DongHang(Long idSanPhamChiTiet, Integer soLuong) {}
}
