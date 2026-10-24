package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.LichLamViec.CaLamViecLichDto;
import com.example.datn.dto.LichLamViec.CaLamViecLichRequest;
import com.example.datn.dto.LichLamViec.LichLamViecDto;
import com.example.datn.dto.LichLamViec.LichLamViecRequest;
import com.example.datn.service.LichLamViecService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class LichLamViecController {

    private final LichLamViecService service;
    private final QuyenGuard quyenGuard;

    @GetMapping("/api/ca-lam-viec-dinh-nghia")
    public List<CaLamViecLichDto> danhSachCa() {
        quyenGuard.canQuanLyNhanVien();
        return service.danhSachCa();
    }

    @PostMapping("/api/ca-lam-viec-dinh-nghia")
    public ResponseEntity<CaLamViecLichDto> themCa(@Valid @RequestBody CaLamViecLichRequest request) {
        quyenGuard.canQuanLyNhanVien();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.themCa(request));
    }

    @PutMapping("/api/ca-lam-viec-dinh-nghia/{id}")
    public CaLamViecLichDto capNhatCa(@PathVariable Long id, @Valid @RequestBody CaLamViecLichRequest request) {
        quyenGuard.canQuanLyNhanVien();
        return service.capNhatCa(id, request);
    }

    @PatchMapping("/api/ca-lam-viec-dinh-nghia/{id}/trang-thai")
    public CaLamViecLichDto doiTrangThaiCa(@PathVariable Long id) {
        quyenGuard.canQuanLyNhanVien();
        return service.doiTrangThaiCa(id);
    }

    @DeleteMapping("/api/ca-lam-viec-dinh-nghia/{id}")
    public ResponseEntity<Void> xoaCa(@PathVariable Long id) {
        quyenGuard.canQuanLyNhanVien();
        service.xoaCa(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/lich-lam-viec")
    public List<LichLamViecDto> danhSachLich(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay
    ) {
        quyenGuard.canQuanLyNhanVien();
        return service.danhSachLich(tuNgay, denNgay);
    }

    @PostMapping("/api/lich-lam-viec")
    public ResponseEntity<LichLamViecDto> themLich(@Valid @RequestBody LichLamViecRequest request) {
        quyenGuard.canQuanLyNhanVien();
        return ResponseEntity.status(HttpStatus.CREATED).body(service.themLich(request));
    }

    @PutMapping("/api/lich-lam-viec/{id}")
    public LichLamViecDto capNhatLich(@PathVariable Long id, @Valid @RequestBody LichLamViecRequest request) {
        quyenGuard.canQuanLyNhanVien();
        return service.capNhatLich(id, request);
    }

    @DeleteMapping("/api/lich-lam-viec/{id}")
    public ResponseEntity<Void> xoaLich(@PathVariable Long id) {
        quyenGuard.canQuanLyNhanVien();
        service.xoaLich(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/api/lich-lam-viec")
    public ResponseEntity<Void> xoaLichTheoNgay(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        quyenGuard.canQuanLyNhanVien();
        service.xoaLichTheoNgay(ngay);
        return ResponseEntity.noContent().build();
    }
}
