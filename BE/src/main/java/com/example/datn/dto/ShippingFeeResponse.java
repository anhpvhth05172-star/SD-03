package com.example.datn.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ShippingFeeResponse {

    private Integer shippingFee;
    private String currency;
    private String message;
    private Integer toProvinceId;
    private Integer toDistrictId;
    private String toWardCode;
    private String toWardName;

    public ShippingFeeResponse(Integer shippingFee, String currency, String message) {
        this.shippingFee = shippingFee;
        this.currency = currency;
        this.message = message;
    }
}
