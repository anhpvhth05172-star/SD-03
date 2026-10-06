package com.example.datn.service;

import com.example.datn.entity.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface DotGiamGiaService {

    Page<DotGiamGia> getPage(Pageable pageable);

    void addDotGiamGia(DotGiamGia dotGiamGia);

    DotGiamGia detailDotGiamGia(Long id);
}
