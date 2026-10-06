package com.example.datn.controller;

import com.example.datn.dto.KhachHangDTO;
import com.example.datn.entity.KhachHang;
import com.example.datn.repository.HoaDonRepository;
import com.example.datn.repository.KhachHangRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private final KhachHangRepository khachHangRepository;
    private final HoaDonRepository hoaDonRepository;

    @GetMapping
    public List<KhachHangDTO> list() {
        Map<Long, Object[]> tongHop = hoaDonRepository.tongHopTheoKhachHang().stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> row
            ));
        return khachHangRepository.findAll().stream()
            .map(k -> {
                Object[] stats = tongHop.get(k.getId());
                long soDon = stats != null ? (Long) stats[1] : 0L;
                BigDecimal tongChiTieu = stats != null ? (BigDecimal) stats[2] : BigDecimal.ZERO;
                return toDTO(k, soDon, tongChiTieu);
            })
            .toList();
    }

    private KhachHangDTO toDTO(KhachHang k, long soDon, BigDecimal tongChiTieu) {
        return new KhachHangDTO(
            k.getId(),
            k.getMaKhachHang(),
            k.getTenKhachHang(),
            k.getTenTaiKhoan(),
            k.getEmail(),
            k.getSoDienThoai(),
            k.getNgayTao(),
            k.getTrangThai(),
            soDon,
            tongChiTieu
        );
    }
}
