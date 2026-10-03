package com.example.datn.controller;

import com.example.datn.dto.SanPhamChiTietResponse;
import com.example.datn.service.SanPhamChiTietService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/san-pham-chi-tiet")
@CrossOrigin(originPatterns = "*")
@RequiredArgsConstructor
public class SanPhamChiTietController {

    private final SanPhamChiTietService sanPhamChiTietService;

    @GetMapping
    public ResponseEntity<Page<SanPhamChiTietResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long idSanPham,
            @RequestParam(required = false) Long idMauSac,
            @RequestParam(required = false) Long idKichCo,
            @RequestParam(required = false) Boolean trangThai
    ) {
        Page<SanPhamChiTietResponse> result = sanPhamChiTietService.getAll(page, size, search, idSanPham, idMauSac, idKichCo, trangThai);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/by-san-pham/{idSanPham}")
    public ResponseEntity<List<SanPhamChiTietResponse>> getBySanPhamId(@PathVariable Long idSanPham) {
        return ResponseEntity.ok(sanPhamChiTietService.getBySanPhamId(idSanPham));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SanPhamChiTietResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sanPhamChiTietService.getById(id));
    }

    @PostMapping("/batch")
    public ResponseEntity<List<SanPhamChiTietResponse>> createBatch(@RequestBody List<com.example.datn.dto.SanPhamChiTietRequest> requests) {
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED).body(sanPhamChiTietService.createBatch(requests));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<SanPhamChiTietResponse> toggleStatus(@PathVariable Long id) {
        return ResponseEntity.ok(sanPhamChiTietService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sanPhamChiTietService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
