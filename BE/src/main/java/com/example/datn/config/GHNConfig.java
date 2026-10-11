package com.example.datn.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
public class GHNConfig {

    @Value("${app.ghn.api-url:https://dev-online-gateway.ghn.vn}")
    private String apiUrl = "https://dev-online-gateway.ghn.vn";

    @Value("${app.ghn.token:}")
    private String token = "";

    @Value("${app.ghn.shop-id:}")
    private String shopId = "";

    @Value("${app.ghn.service-type-id:2}")
    private int serviceTypeId = 2;

    @Value("${app.ghn.package-length-cm:30}")
    private int packageLengthCm = 30;

    @Value("${app.ghn.package-width-cm:20}")
    private int packageWidthCm = 20;

    @Value("${app.ghn.package-height-cm:15}")
    private int packageHeightCm = 15;

    @Value("${app.ghn.default-item-weight-g:500}")
    private long defaultItemWeightG = 500;

    @Value("${app.ghn.max-weight-g:30000}")
    private long maxWeightG = 30000;

    @Value("${app.ghn.insurance-max-vnd:5000000}")
    private long insuranceMaxVnd = 5000000;

    @Value("${app.ghn.from-district-id:}")
    private Integer fromDistrictId;

    @Value("${app.ghn.from-ward-code:}")
    private String fromWardCode;

    @Value("${app.ghn.timeout-ms:10000}")
    private int timeoutMs = 10000;

    public void kiemTraCauHinh() {
        if (apiUrl == null || apiUrl.isBlank()) {
            throw new IllegalArgumentException("Chưa cấu hình GHN: thiếu biến môi trường GHN_API_URL");
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Chưa cấu hình GHN: thiếu biến môi trường GHN_TOKEN");
        }
        if (shopId == null || shopId.isBlank()) {
            throw new IllegalArgumentException("Chưa cấu hình GHN: thiếu biến môi trường GHN_SHOP_ID");
        }
    }
}
