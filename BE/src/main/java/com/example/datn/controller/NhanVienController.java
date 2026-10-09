package com.example.datn.controller;

import com.example.datn.dto.NhanVienDTO;
import com.example.datn.dto.NhanVienFormDataResponse;
import com.example.datn.dto.NhanVienRequest;
import com.example.datn.dto.PageResponse;
import com.example.datn.service.NhanVienService;
import java.time.LocalDate;
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
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final NhanVienService nhanVienService;

    @GetMapping
    public PageResponse<NhanVienDTO> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long idVaiTro,
        @RequestParam(required = false) String trangThai,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return nhanVienService.list(keyword, idVaiTro, trangThai, tuNgay, denNgay, page, size);
    }

    @GetMapping("/form-data")
    public NhanVienFormDataResponse formData() {
        return nhanVienService.formData();
    }

    @GetMapping("/{id}")
    public NhanVienDTO get(@PathVariable Long id) {
        return nhanVienService.get(id);
    }

    @PostMapping
    public ResponseEntity<NhanVienDTO> create(@RequestBody NhanVienRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(nhanVienService.create(request));
    }

    @PutMapping("/{id}")
    public NhanVienDTO update(@PathVariable Long id, @RequestBody NhanVienRequest request) {
        return nhanVienService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        nhanVienService.delete(id);
        return ResponseEntity.noContent().build();
    }
}