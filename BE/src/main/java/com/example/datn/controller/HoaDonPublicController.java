package com.example.datn.controller;

import com.example.datn.dto.HoaDon.HoaDonDTO;
import com.example.datn.service.HoaDonService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/hoa-don")
@RequiredArgsConstructor
public class HoaDonPublicController {

    private final HoaDonService hoaDonService;

    @GetMapping("/tra-cuu")
    public ResponseEntity<HoaDonDTO> traCuuPublic(@RequestParam String maHoaDon) {
        if (maHoaDon == null || maHoaDon.trim().isEmpty()) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(hoaDonService.traCuuPublic(maHoaDon.trim()));
    }
}
