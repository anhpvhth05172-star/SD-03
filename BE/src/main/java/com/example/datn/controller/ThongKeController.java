package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.dto.ThongKe.TrangThaiDonDTO;
import com.example.datn.service.ThongKeService;
import java.time.LocalDate;
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
public class ThongKeController {

    private final ThongKeService thongKeService;
    private final QuyenGuard quyenGuard;

    @GetMapping("/tong-quan")
    public TongQuanDTO tongQuan(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false) String thuongHieu
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.tongQuan(tu, den, thuongHieu);
    }

    @GetMapping("/trang-thai-don")
    public List<TrangThaiDonDTO> trangThaiDon(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false) String thuongHieu
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.trangThaiDon(tu, den, thuongHieu);
    }
}
