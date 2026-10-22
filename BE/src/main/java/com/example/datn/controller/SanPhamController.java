package com.example.datn.controller;

import com.example.datn.dto.Common.PageResponse;
import com.example.datn.dto.SanPham.SanPhamRequest;
import com.example.datn.dto.SanPham.SanPhamResponse;
import com.example.datn.dto.SanPham.SmartProductSaveRequest;
import com.example.datn.dto.SanPham.SmartProductSaveResponse;
import com.example.datn.service.SanPhamService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/san-pham")
@RequiredArgsConstructor
public class SanPhamController {

    private final SanPhamService sanPhamService;
    private final com.example.datn.service.GeminiService geminiService;

    @PostMapping("/ai-generate-content")
    public ResponseEntity<java.util.Map<String, String>> aiGenerateContent(
            @RequestBody java.util.Map<String, String> request
    ) {
        String ten = request.get("tenSanPham");
        String thuongHieu = request.get("thuongHieu");
        String loaiGiay = request.get("loaiGiay");
        String chatLieu = request.get("chatLieu");
        String kieuDang = request.get("kieuDang");
        String doiTuong = request.get("doiTuong");
        java.util.Map<String, String> result = geminiService.sinhNoiDungSanPham(ten, thuongHieu, loaiGiay, chatLieu, kieuDang, doiTuong);
        return ResponseEntity.ok(result);
    }

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

    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] bytes = sanPhamService.downloadExcelTemplate();
        return ResponseEntity.ok()
                .header(org.springframework.http.HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"Mau_Import_San_Pham.xlsx\"")
                .contentType(org.springframework.http.MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(bytes);
    }

    @PostMapping(value = "/excel/import", consumes = org.springframework.http.MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<com.example.datn.dto.SanPham.ProductExcelImportResult> importExcel(
            @RequestParam("file") org.springframework.web.multipart.MultipartFile file
    ) {
        com.example.datn.dto.SanPham.ProductExcelImportResult result = sanPhamService.importFromExcel(file);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        sanPhamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
