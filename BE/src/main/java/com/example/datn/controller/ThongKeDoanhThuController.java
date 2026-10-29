package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.ThongKe.DoanhThuDTO;
import com.example.datn.repository.HoaDonRepository;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thong-ke")
@RequiredArgsConstructor
public class ThongKeDoanhThuController {

    private final HoaDonRepository hoaDonRepository;
    private final QuyenGuard quyenGuard;

    @GetMapping("/doanh-thu")
    public DoanhThuDTO doanhThu(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den
    ) {
        quyenGuard.canXemThongKe();
        if (den.isBefore(tu)) {
            throw new IllegalArgumentException("Ngày kết thúc phải từ ngày bắt đầu trở đi");
        }
        var batDau = tu.atStartOfDay();
        var ketThucKhongBaoGom = den.plusDays(1).atTime(LocalTime.MIDNIGHT);
        return new DoanhThuDTO(
            hoaDonRepository.tongDoanhThuTheoTrangThai(List.of("DA_GIAO_HANG", "DA_HOAN_THANH"), batDau, ketThucKhongBaoGom),
            hoaDonRepository.tongDoanhThuTheoTrangThai(List.of("DA_XAC_NHAN"), batDau, ketThucKhongBaoGom)
        );
    }
}
