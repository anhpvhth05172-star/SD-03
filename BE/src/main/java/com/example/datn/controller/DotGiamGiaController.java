package com.example.datn.controller;

import com.example.datn.dto.DotGiamGiaDTO;
import com.example.datn.entity.DotGiamGia;
import com.example.datn.repository.DotGiamGiaRepository;
import com.example.datn.repository.PhieuGiamGiaRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dot-giam-gia")
@RequiredArgsConstructor
public class DotGiamGiaController {

    private final DotGiamGiaRepository dotGiamGiaRepository;
    private final PhieuGiamGiaRepository phieuGiamGiaRepository;

    @GetMapping
    public List<DotGiamGiaDTO> list() {
        return dotGiamGiaRepository.findAll().stream()
            .map(this::toDTO)
            .toList();
    }

    private DotGiamGiaDTO toDTO(DotGiamGia dot) {
        return new DotGiamGiaDTO(
            dot.getId(),
            dot.getMaDotGiamGia(),
            dot.getTenDotGiamGia(),
            dot.getPhanTramGiam(),
            dot.getNgayBatDau(),
            dot.getNgayKetThuc(),
            dot.getTrangThai(),
            phieuGiamGiaRepository.countByDotGiamGiaId(dot.getId())
        );
    }
}
