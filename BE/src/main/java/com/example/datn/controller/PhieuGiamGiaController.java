package com.example.datn.controller;

import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaDTO;
import com.example.datn.dto.PhieuGiamGia.PhieuSuDungCuaToiDTO;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.repository.LichSuSuDungPhieuGiamGiaRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.util.PhieuGiamGiaUtil;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/phieu-giam-gia")
@RequiredArgsConstructor
public class PhieuGiamGiaController {

    private final PhieuGiamGiaRepository phieuGiamGiaRepository;
    private final LichSuSuDungPhieuGiamGiaRepository lichSuSuDungRepository;

    @GetMapping
    public List<PhieuGiamGiaDTO> list() {
        LocalDateTime now = LocalDateTime.now();
        return phieuGiamGiaRepository.findAllWithDot().stream()
            .filter(p -> !"CA_NHAN".equalsIgnoreCase(p.getPhamVi()))
            .filter(p -> Boolean.TRUE.equals(p.getTrangThai()))
            .filter(p -> p.getNgayBatDau() == null || !now.isBefore(p.getNgayBatDau()))
            .filter(p -> p.getNgayKetThuc() == null || !now.isAfter(p.getNgayKetThuc()))
            .map(this::toDTO)
            .toList();
    }

    @GetMapping("/cua-toi")
    public List<PhieuSuDungCuaToiDTO> cuaToi() {
        Object principal = SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Long idKhachHang = (Long) principal;
        Map<Long, Long> dem = lichSuSuDungRepository.demTheoPhieuCuaKhachHang(idKhachHang).stream()
            .collect(Collectors.toMap(
                row -> (Long) row[0],
                row -> (Long) row[1]
            ));
        return phieuGiamGiaRepository.findAll().stream()
            .map(p -> new PhieuSuDungCuaToiDTO(
                p.getId(),
                p.getMaPhieuGiamGia(),
                PhieuGiamGiaUtil.tenHienThi(p.getLoaiGiamGia(), p.getGiaTriGiam()),
                p.getGioiHanMoiTaiKhoan() != null ? p.getGioiHanMoiTaiKhoan() : 1,
                dem.getOrDefault(p.getId(), 0L)
            ))
            .toList();
    }

    private PhieuGiamGiaDTO toDTO(PhieuGiamGia p) {
        PhieuGiamGiaDTO.DotGiamGiaDTO dot = p.getDotGiamGia() != null
            ? new PhieuGiamGiaDTO.DotGiamGiaDTO(
                p.getDotGiamGia().getId(),
                p.getDotGiamGia().getMaDotGiamGia(),
                p.getDotGiamGia().getTenDotGiamGia()
            )
            : null;
        return new PhieuGiamGiaDTO(
            p.getId(),
            p.getMaPhieuGiamGia(),
            PhieuGiamGiaUtil.tenHienThi(p.getLoaiGiamGia(), p.getGiaTriGiam()),
            p.getLoaiGiamGia(),
            p.getGiaTriGiam(),
            p.getGiamToiDa(),
            p.getHoaDonToiThieu(),
            p.getNgayBatDau(),
            p.getNgayKetThuc(),
            p.getSoLuong(),
            p.getSoLuongDaSuDung(),
            p.getGioiHanMoiTaiKhoan() != null ? p.getGioiHanMoiTaiKhoan() : 1,
            p.getTrangThai(),
            dot
        );
    }
}
