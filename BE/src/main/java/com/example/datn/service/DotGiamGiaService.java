package com.example.datn.service;

import com.example.datn.entity.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;

public interface DotGiamGiaService {

    Page<DotGiamGia> getPage(Pageable pageable);

    void addDotGiamGia(DotGiamGia dotGiamGia);

    DotGiamGia detailDotGiamGia(Long id);

    void updateDotGiamGia(DotGiamGia dotGiamGia);

    Page<DotGiamGia> searching(String ten, Boolean trangThai,
                               LocalDateTime from, LocalDateTime to, Pageable pageable);
}
