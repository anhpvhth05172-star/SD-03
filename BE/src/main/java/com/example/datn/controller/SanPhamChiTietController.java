package com.example.datn.controller;

import com.example.datn.dto.SanPham.SanPhamChiTietRequest;
import com.example.datn.dto.SanPham.SanPhamChiTietResponse;
import com.example.datn.service.SanPhamChiTietService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/san-pham-chi-tiet")
@RequiredArgsConstructor
@Validated
public class SanPhamChiTietController {

    private final SanPhamChiTietService sanPhamChiTietService;
    private final com.example.datn.service.GeminiService geminiService;

    @PostMapping("/ai-visual-search")
    public ResponseEntity<java.util.Map<String, Object>> aiVisualSearch(
            @RequestBody java.util.Map<String, String> request
    ) {
        String imageBase64 = request.get("imageBase64");
        String mimeType = request.get("mimeType");
        java.util.Map<String, Object> aiResult = geminiService.nhanDienGiayQuaHinhAnh(imageBase64, mimeType);

        // Khớp biến thể qua thuật toán xếp hạng đa tầng của AI
        List<SanPhamChiTietResponse> matchingVariants = sanPhamChiTietService.matchVariantsByAI(aiResult);

        java.util.Map<String, Object> response = new java.util.HashMap<>(aiResult);
        response.put("matchingVariants", matchingVariants);
        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<com.example.datn.dto.Common.PageResponse<SanPhamChiTietResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) Long idSanPham,
            @RequestParam(required = false) Long idMauSac,
            @RequestParam(required = false) Long idKichCo,
            @RequestParam(required = false) Boolean trangThai
    ) {
        Page<SanPhamChiTietResponse> result = sanPhamChiTietService.getAll(page, size, search, idSanPham, idMauSac, idKichCo, trangThai);
        return ResponseEntity.ok(com.example.datn.dto.Common.PageResponse.from(result));
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
    public ResponseEntity<List<SanPhamChiTietResponse>> createBatch(
            @Valid @RequestBody List<SanPhamChiTietRequest> requests
    ) {
        return ResponseEntity.ok(sanPhamChiTietService.createBatch(requests));
    }

    @PutMapping("/{id}")
    public ResponseEntity<SanPhamChiTietResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SanPhamChiTietRequest request
    ) {
        return ResponseEntity.ok(sanPhamChiTietService.update(id, request));
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
