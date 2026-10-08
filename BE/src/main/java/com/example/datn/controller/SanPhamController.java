package com.example.datn.controller;

import com.example.datn.dto.PageResponse;
import com.example.datn.dto.SanPhamRequest;
import com.example.datn.dto.SanPhamResponse;
import com.example.datn.dto.SmartProductSaveRequest;
import com.example.datn.dto.SmartProductSaveResponse;
import com.example.datn.service.SanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/san-pham")
@CrossOrigin(originPatterns = "*")
@RequiredArgsConstructor
public class SanPhamController {

    private final SanPhamService sanPhamService;

    @GetMapping
    public ResponseEntity<PageResponse<SanPhamResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Long idThuongHieu,
            @RequestParam(required = false) Long idLoaiGiay,
            @RequestParam(required = false) Long idChatLieu,
            @RequestParam(required = false) Long idKieuDang,
            @RequestParam(required = false) Long idXuatXu,
            @RequestParam(required = false) String doiTuong,
            @RequestParam(required = false) Boolean trangThai
    ) {
        Page<SanPhamResponse> result = sanPhamService.getAll(page, size, keyword, idThuongHieu, idLoaiGiay, idChatLieu, idKieuDang, idXuatXu, doiTuong, trangThai);
        return ResponseEntity.ok(PageResponse.from(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<SanPhamResponse> getById(@PathVariable Long id) {
        return ResponseEntity.ok(sanPhamService.getById(id));
    }

    @PostMapping
    public ResponseEntity<SanPhamResponse> create(@Valid @RequestBody SanPhamRequest request) {
        SanPhamResponse response = sanPhamService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/smart-save")
    public ResponseEntity<SmartProductSaveResponse> smartSave(
            @Valid @RequestBody SmartProductSaveRequest request
    ) {
        SmartProductSaveResponse response = sanPhamService.smartSave(request);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<SanPhamResponse> update(
            @PathVariable Long id,
            @Valid @RequestBody SanPhamRequest request
    ) {
        SanPhamResponse response = sanPhamService.update(id, request);
        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<SanPhamResponse> toggleStatus(@PathVariable Long id) {
        SanPhamResponse response = sanPhamService.toggleStatus(id);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sanPhamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
