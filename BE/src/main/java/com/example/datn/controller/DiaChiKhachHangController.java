package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.DiaChiKhachHangDTO;
import com.example.datn.dto.DiaChiKhachHangRequest;
import com.example.datn.service.KhachHangService;
import java.util.List;
import lombok.RequiredArgsConstructor;
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
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/khach-hang/{idKhachHang}/dia-chi")
@RequiredArgsConstructor
public class DiaChiKhachHangController {

    private final KhachHangService khachHangService;
    private final QuyenGuard quyenGuard;

    @GetMapping
    public List<DiaChiKhachHangDTO> list(@PathVariable Long idKhachHang) {
        return khachHangService.dsDiaChi(idKhachHang);
    }

    @PostMapping
    public ResponseEntity<DiaChiKhachHangDTO> create(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long idKhachHang,
        @RequestBody DiaChiKhachHangRequest request
    ) {
        quyenGuard.canQuanLyKhachHang(vaiTro);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(khachHangService.themDiaChi(idKhachHang, request));
    }

    @PutMapping("/{id}")
    public DiaChiKhachHangDTO update(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long idKhachHang,
        @PathVariable("id") Long id,
        @RequestBody DiaChiKhachHangRequest request
    ) {
        quyenGuard.canQuanLyKhachHang(vaiTro);
        return khachHangService.capNhatDiaChi(idKhachHang, id, request);
    }

    @PutMapping("/{id}/mac-dinh")
    public DiaChiKhachHangDTO datMacDinh(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long idKhachHang,
        @PathVariable("id") Long id
    ) {
        quyenGuard.canQuanLyKhachHang(vaiTro);
        return khachHangService.datMacDinh(idKhachHang, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @RequestHeader(value = QuyenGuard.HEADER_VAI_TRO, required = false) String vaiTro,
        @PathVariable Long idKhachHang,
        @PathVariable("id") Long id
    ) {
        quyenGuard.canQuanLyKhachHang(vaiTro);
        khachHangService.xoaDiaChi(idKhachHang, id);
        return ResponseEntity.noContent().build();
    }
}
