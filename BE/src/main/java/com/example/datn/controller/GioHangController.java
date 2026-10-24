package com.example.datn.controller;

import com.example.datn.dto.GioHang.AddGioHangResponse;
import com.example.datn.dto.GioHang.GioHangResponse;
import com.example.datn.dto.GioHang.ThemGioHangRequest;
import com.example.datn.dto.GioHang.UpdateGioHangRequest;
import com.example.datn.service.GioHangService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/gio-hang")
@RequiredArgsConstructor
public class GioHangController {

    private final GioHangService gioHangService;

    @PostMapping("/them")
    public ResponseEntity<AddGioHangResponse> them(@Valid @RequestBody ThemGioHangRequest request) {
        return ResponseEntity.ok(gioHangService.themVaoGioHang(request));
    }

    @GetMapping
    public ResponseEntity<GioHangResponse> xem(@RequestParam Long idKhachHang) {
        return ResponseEntity.ok(gioHangService.xemGioHang(idKhachHang));
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Integer>> dem(@RequestParam(required = false) Long idKhachHang) {
        return ResponseEntity.ok(Map.of("tongSoLuong", gioHangService.demSoLuong(idKhachHang)));
    }

    @PutMapping("/items/{id}")
    public ResponseEntity<GioHangResponse> capNhat(@PathVariable Integer id,
                                                   @Valid @RequestBody UpdateGioHangRequest request) {
        return ResponseEntity.ok(gioHangService.capNhatSoLuong(id, request));
    }

    @DeleteMapping("/items/{id}")
    public ResponseEntity<GioHangResponse> xoaMot(@PathVariable Integer id,
                                                  @RequestParam Long idKhachHang) {
        return ResponseEntity.ok(gioHangService.xoaSanPham(id, idKhachHang));
    }

    @DeleteMapping
    public ResponseEntity<GioHangResponse> xoaHet(@RequestParam Long idKhachHang) {
        return ResponseEntity.ok(gioHangService.xoaTatCa(idKhachHang));
    }
}
