package com.example.datn.controller;

import com.example.datn.dto.SanPhamChiTietDTO;
import com.example.datn.dto.SanPhamChiTietRequest;
import com.example.datn.dto.SanPhamDTO;
import com.example.datn.dto.SanPhamRequest;
import com.example.datn.dto.ThuocTinhResponse;
import com.example.datn.service.SanPhamService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class SanPhamController {

    private final SanPhamService sanPhamService;

    @GetMapping("/san-pham")
    public List<SanPhamDTO> list(@RequestParam(required = false) String keyword) {
        return sanPhamService.layDanhSach(keyword);
    }

    @PostMapping("/san-pham")
    public SanPhamDTO create(@RequestBody SanPhamRequest request) {
        return sanPhamService.taoSanPham(request);
    }

    @GetMapping("/san-pham/{id}")
    public SanPhamDTO detail(@PathVariable Long id) {
        return sanPhamService.laySanPham(id);
    }

    @GetMapping("/san-pham/{id}/chi-tiet")
    public List<SanPhamChiTietDTO> variants(@PathVariable Long id) {
        return sanPhamService.layBienThe(id);
    }

    @PostMapping("/san-pham/{id}/chi-tiet")
    public SanPhamChiTietDTO createVariant(
        @PathVariable Long id,
        @RequestBody SanPhamChiTietRequest request
    ) {
        return sanPhamService.taoBienThe(id, request);
    }

    @GetMapping("/thuoc-tinh")
    public ThuocTinhResponse thuocTinh() {
        return sanPhamService.layThuocTinh();
    }
}
