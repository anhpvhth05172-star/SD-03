package com.example.datn.controller;

import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaDTO;
import com.example.datn.dto.PhieuGiamGia.PhieuSuDungCuaToiDTO;
import com.example.datn.entity.KhachHangPhieuGiamGia;
import com.example.datn.entity.LichSuSuDungPhieuGiamGia;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.repository.KhachHangPhieuGiamGiaRepository;
import com.example.datn.repository.LichSuSuDungPhieuGiamGiaRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.util.PhieuGiamGiaUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
    private final KhachHangPhieuGiamGiaRepository khachHangPhieuGiamGiaRepository;

    @GetMapping
    public List<PhieuGiamGiaDTO> list() {
        LocalDateTime now = LocalDateTime.now();
        List<KhachHangPhieuGiamGia> allKhPg = khachHangPhieuGiamGiaRepository.findAll();
        List<LichSuSuDungPhieuGiamGia> allLichSu = lichSuSuDungRepository.findAll();

        Map<Long, Map<Long, Long>> usageByPhieuAndKhach = allLichSu.stream()
            .filter(l -> l.getPhieuGiamGia() != null && l.getKhachHang() != null
                && l.getPhieuGiamGia().getId() != null && l.getKhachHang().getId() != null)
            .collect(Collectors.groupingBy(
                l -> l.getPhieuGiamGia().getId(),
                Collectors.groupingBy(
                    l -> l.getKhachHang().getId(),
                    Collectors.counting()
                )
            ));

        Map<Long, List<Long>> khChuaDungMap = allKhPg.stream()
            .filter(r -> Boolean.TRUE.equals(r.getTrangThai()) && r.getNgaySuDung() == null
                && r.getPhieuGiamGia() != null && r.getKhachHang() != null)
            .collect(Collectors.groupingBy(
                r -> r.getPhieuGiamGia().getId(),
                Collectors.mapping(r -> r.getKhachHang().getId(), Collectors.toList())
            ));

        return phieuGiamGiaRepository.findAllWithDot().stream()
            .filter(p -> Boolean.TRUE.equals(p.getTrangThai()))
            .filter(p -> p.getNgayBatDau() == null || !now.isBefore(p.getNgayBatDau()))
            .filter(p -> p.getNgayKetThuc() == null || !now.isAfter(p.getNgayKetThuc()))
            .map(p -> toDTO(p, allKhPg, usageByPhieuAndKhach, khChuaDungMap))
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

    private PhieuGiamGiaDTO toDTO(
        PhieuGiamGia p,
        List<KhachHangPhieuGiamGia> allKhPg,
        Map<Long, Map<Long, Long>> usageByPhieuAndKhach,
        Map<Long, List<Long>> khChuaDungMap
    ) {
        PhieuGiamGiaDTO.DotGiamGiaDTO dot = p.getDotGiamGia() != null
            ? new PhieuGiamGiaDTO.DotGiamGiaDTO(
                p.getDotGiamGia().getId(),
                p.getDotGiamGia().getMaDotGiamGia(),
                p.getDotGiamGia().getTenDotGiamGia()
            )
            : null;
        int limit = p.getGioiHanMoiTaiKhoan() != null && p.getGioiHanMoiTaiKhoan() > 0
            ? p.getGioiHanMoiTaiKhoan() : 1;
        Map<Long, Long> khUsage = usageByPhieuAndKhach.getOrDefault(p.getId(), Map.of());
        Set<Long> daDungSet = new HashSet<>();
        khUsage.forEach((khId, count) -> {
            if (count >= limit) {
                daDungSet.add(khId);
            }
        });
        if ("CA_NHAN".equalsIgnoreCase(p.getPhamVi())) {
            allKhPg.stream()
                .filter(r -> r.getPhieuGiamGia() != null && p.getId().equals(r.getPhieuGiamGia().getId()) && r.getKhachHang() != null)
                .forEach(r -> {
                    if (Boolean.FALSE.equals(r.getTrangThai()) || r.getNgaySuDung() != null) {
                        daDungSet.add(r.getKhachHang().getId());
                    }
                });
        }
        List<Long> chuaDung = khChuaDungMap.getOrDefault(p.getId(), List.of()).stream()
            .filter(khId -> !daDungSet.contains(khId))
            .toList();

        PhieuGiamGiaDTO dto = new PhieuGiamGiaDTO(
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
            limit,
            p.getTrangThai(),
            dot
        );
        dto.setPhamVi(p.getPhamVi());
        dto.setIdKhachHangsChuaDung(chuaDung);
        dto.setIdKhachHangsDaDung(new ArrayList<>(daDungSet));
        return dto;
    }
}
