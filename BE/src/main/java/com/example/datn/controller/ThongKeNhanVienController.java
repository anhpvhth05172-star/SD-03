package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.Common.PageResponse;
import com.example.datn.dto.ThongKe.NhanVienThongKeDTO;
import com.example.datn.service.NhanVienService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thong-ke")
@RequiredArgsConstructor
public class ThongKeNhanVienController {

    private final NhanVienService nhanVienService;
    private final QuyenGuard quyenGuard;

    @GetMapping("/nhan-vien")
    public PageResponse<NhanVienThongKeDTO> danhSachNhanVien(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "50") int size
    ) {
        quyenGuard.canXemThongKe();
        var result = nhanVienService.list(null, null, null, null, null, page, size);
        return new PageResponse<>(
            result.content().stream()
                .map(nv -> new NhanVienThongKeDTO(nv.getId(), nv.getMaNhanVien(), nv.getTenTaiKhoan(),
                    nv.getTenVaiTro(), nv.getAnhNhanVien()))
                .toList(),
            result.page(), result.size(), result.totalElements(), result.totalPages()
        );
    }
}
