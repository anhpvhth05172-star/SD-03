package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.KhachHangDTO;
import com.example.datn.dto.KhachHangRequest;
import com.example.datn.dto.PageResponse;
import com.example.datn.service.KhachHangService;
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
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private final KhachHangService khachHangService;
    private final QuyenGuard quyenGuard;

    @GetMapping
    public PageResponse<KhachHangDTO> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String hangThanhVien,
        @RequestParam(required = false) String trangThai,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return khachHangService.list(keyword, hangThanhVien, trangThai, tuNgay, denNgay, page, size);
    }

    @GetMapping("/ma-tu-dong")
    public Map<String, String> maKhachHangTuDong(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @RequestParam(required = false) String ten
    ) {
        quyenGuard.canQuanLyKhachHang(vaiTro);
        return Map.of("ma", khachHangService.maKhachHangTuDong(ten));
    }

    @GetMapping("/{id}")
    public KhachHangDTO get(@PathVariable Long id) {
        return khachHangService.get(id);
    }

    @PostMapping
    public ResponseEntity<KhachHangDTO> create(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @RequestBody KhachHangRequest request
    ) {
        quyenGuard.canQuanLyKhachHang(vaiTro);
        return ResponseEntity.status(HttpStatus.CREATED).body(khachHangService.create(request));
    }

    @PutMapping("/{id}")
    public KhachHangDTO update(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long id,
        @RequestBody KhachHangRequest request
    ) {
        quyenGuard.canQuanLyKhachHang(vaiTro);
        return khachHangService.update(id, request);
    }

    @PutMapping("/{id}/trang-thai")
    public KhachHangDTO capNhatTrangThai(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long id,
        @RequestParam String trangThai
    ) {
        quyenGuard.canQuanLyKhachHang(vaiTro);
        return khachHangService.capNhatTrangThai(id, trangThai);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long id
    ) {
        quyenGuard.canAdmin(vaiTro);
        khachHangService.delete(id);
        return ResponseEntity.noContent().build();
    }
}