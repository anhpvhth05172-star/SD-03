package com.example.datn.controller;

import com.example.datn.dto.ShippingAddressDTO;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.dto.ShippingFeeResponse;
import com.example.datn.dto.ShippingServiceDTO;
import com.example.datn.dto.ShippingWardDTO;
import com.example.datn.service.GHNService;
import com.example.datn.service.GhnDiaChiMapper;
import com.example.datn.service.GhnDiaChiMapper.KetQuaAnhXach;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/shipping")
public class ShippingController {

    private final GHNService ghnService;
    private final GhnDiaChiMapper ghnDiaChiMapper;

    public ShippingController(GHNService ghnService, GhnDiaChiMapper ghnDiaChiMapper) {
        this.ghnService = ghnService;
        this.ghnDiaChiMapper = ghnDiaChiMapper;
    }

    @GetMapping("/provinces")
    public List<ShippingAddressDTO> provinces() {
        return ghnService.getTinhThanhMoi();
    }

    @GetMapping("/districts")
    public List<ShippingAddressDTO> districts(@RequestParam(required = false) Integer provinceId) {
        return ghnService.getQuanHuyen(provinceId);
    }

    @GetMapping("/wards")
    public List<ShippingAddressDTO> wards(@RequestParam(required = false) Integer provinceId) {
        return ghnService.getPhuongXaMoi(provinceId);
    }

    @GetMapping("/services")
    public List<ShippingServiceDTO> services(@RequestParam Integer toDistrictId) {
        return ghnService.getDichVu(toDistrictId);
    }

    @PostMapping("/fee")
    public ShippingFeeResponse fee(@RequestBody ShippingFeeRequest request) {
        boolean canThieuMa = request.getToDistrictId() == null
            || request.getToWardCode() == null
            || request.getToWardCode().isBlank();
        Integer tinhIdLegacy = null;
        if (canThieuMa) {
            if (ghnService.daCauHinhGhn()) {
                KetQuaAnhXach kq = ghnDiaChiMapper.anhXach(
                    request.getToProvinceName(), request.getToWardName());
                request.setToDistrictId(kq.quanId());
                request.setToWardCode(kq.maPhuong());
                tinhIdLegacy = kq.tinhId();
            } else {
                request.setToDistrictId(1);
                request.setToWardCode("001");
                tinhIdLegacy = 1;
            }
        }
        ShippingFeeResponse response = ghnService.tinhPhiResponse(request);
        response.setToProvinceId(tinhIdLegacy);
        response.setToDistrictId(request.getToDistrictId());
        response.setToWardCode(request.getToWardCode());
        response.setToWardName(request.getToWardName());
        return response;
    }
}
