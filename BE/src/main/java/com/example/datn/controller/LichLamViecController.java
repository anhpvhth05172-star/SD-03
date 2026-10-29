package com.example.datn.controller;

import com.example.datn.dto.LichLamViec.ImportLichRequest;
import com.example.datn.dto.LichLamViec.ImportLichResponse;
import com.example.datn.dto.LichLamViec.LichLamViecDTO;
import com.example.datn.dto.LichLamViec.LichLamViecRequest;
import com.example.datn.dto.LichLamViec.XepTuDongRequest;
import com.example.datn.dto.LichLamViec.XepTuDongResponse;
import com.example.datn.service.LichLamViecService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/lich-lam-viec")
@RequiredArgsConstructor
public class LichLamViecController {

    private final LichLamViecService service;

    @GetMapping
    public ResponseEntity<List<LichLamViecDTO>> list(
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false) String tim,
        @RequestParam(required = false) Long idCa,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        return ResponseEntity.ok(service.list(tu, den, tim, idCa, ngay));
    }

    @PostMapping
    public ResponseEntity<LichLamViecDTO> create(@RequestBody LichLamViecRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<LichLamViecDTO> update(@PathVariable Long id, @RequestBody LichLamViecRequest req) {
        return ResponseEntity.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteTheoNgay(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate ngay
    ) {
        service.deleteTheoNgay(ngay);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/xep-tu-dong")
    public ResponseEntity<XepTuDongResponse> xepTuDong(@RequestBody XepTuDongRequest req) {
        return ResponseEntity.ok(service.xepTuDong(req.ngay()));
    }

    @PostMapping("/import")
    public ResponseEntity<ImportLichResponse> importLich(@RequestBody ImportLichRequest req) {
        return ResponseEntity.ok(service.importLich(req));
    }
}
