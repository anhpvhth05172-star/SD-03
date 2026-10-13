package com.example.datn.controller;

import com.example.datn.dto.ShippingAddressDTO;
import com.example.datn.dto.ShippingFeeRequest;
import com.example.datn.dto.ShippingFeeResponse;
import com.example.datn.dto.ShippingWardDTO;
import com.example.datn.service.GHNService;
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
@RequiredArgsConstructor
public class ShippingController {

    private final GHNService ghnService;

    @GetMapping("/provinces")
    public List<ShippingAddressDTO> provinces() {
        return ghnService.getTinhThanh();
    }

    @GetMapping("/districts")
    public List<ShippingAddressDTO> districts(@RequestParam(required = false) Integer provinceId) {
        return ghnService.getQuanHuyen(provinceId);
    }

    @GetMapping("/wards")
    public List<ShippingWardDTO> wards(@RequestParam(required = false) Integer districtId) {
        return ghnService.getPhuongXa(districtId);
    }

    @PostMapping("/fee")
    public ShippingFeeResponse fee(@RequestBody ShippingFeeRequest request) {
        return ghnService.tinhPhiResponse(request);
    }
}
