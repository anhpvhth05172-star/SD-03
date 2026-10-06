package com.example.datn.service;

import com.example.datn.dto.PhieuGiamGiaDTO;
import com.example.datn.entity.DotGiamGia;
import com.example.datn.entity.PhieuGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

public interface PhieuGiamGiaService {

    Page<PhieuGiamGia> getPageList(Pageable pageable);

    void addPhieuGiamGia(PhieuGiamGia phieuGiamGia);

    PhieuGiamGia detail(Long id);
}
