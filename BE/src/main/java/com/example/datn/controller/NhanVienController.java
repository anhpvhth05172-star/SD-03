package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.NhanVienDTO;
import com.example.datn.dto.NhanVienFormDataResponse;
import com.example.datn.dto.NhanVienRequest;
import com.example.datn.dto.PageResponse;
import com.example.datn.service.NhanVienService;
import java.time.LocalDate;
import java.util.Map;
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
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final NhanVienService nhanVienService;
    private final QuyenGuard quyenGuard;

    @GetMapping
    public PageResponse<NhanVienDTO> list(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long idVaiTro,
        @RequestParam(required = false) String trangThai,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        quyenGuard.canQuanLyNhanVien(vaiTro);
        return nhanVienService.list(keyword, idVaiTro, trangThai, tuNgay, denNgay, page, size);
    }

    @GetMapping("/form-data")
    public NhanVienFormDataResponse formData(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro
    ) {
        quyenGuard.canQuanLyNhanVien(vaiTro);
        return nhanVienService.formData();
    }

    @GetMapping("/ma-tu-dong")
    public Map<String, String> maNhanVienTuDong(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @RequestParam(required = false) String ten
    ) {
        quyenGuard.canQuanLyNhanVien(vaiTro);
        return Map.of("ma", nhanVienService.maNhanVienTuDong(ten));
    }

    @GetMapping("/{id}")
    public NhanVienDTO get(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long id
    ) {
        quyenGuard.canQuanLyNhanVien(vaiTro);
        return nhanVienService.get(id);
    }

    @PostMapping
    public ResponseEntity<NhanVienDTO> create(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @RequestBody NhanVienRequest request
    ) {
        quyenGuard.canQuanLyNhanVien(vaiTro);
        return ResponseEntity.status(HttpStatus.CREATED).body(nhanVienService.create(request));
    }

    @PutMapping("/{id}")
    public NhanVienDTO update(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long id,
        @RequestBody NhanVienRequest request
    ) {
        quyenGuard.canQuanLyNhanVien(vaiTro);
        return nhanVienService.update(id, request);
    }

    @PutMapping("/{id}/trang-thai")
    public NhanVienDTO capNhatTrangThai(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long id,
        @RequestParam String trangThai
    ) {
        quyenGuard.canQuanLyNhanVien(vaiTro);
        return nhanVienService.capNhatTrangThai(id, trangThai);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long id
    ) {
        quyenGuard.canQuanLyNhanVien(vaiTro);
        nhanVienService.delete(id);
        return ResponseEntity.noContent().build();
    }
}