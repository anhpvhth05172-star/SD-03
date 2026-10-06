package com.example.datn.service.impls;

import com.example.datn.entity.DotGiamGia;
import com.example.datn.repository.DotGiamGiaRepository;
import com.example.datn.service.DotGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class DotGiamGiaServiceImpl implements DotGiamGiaService {

    @Autowired
    private DotGiamGiaRepository dotGiamGiaRepository;


    @Override
    public Page<DotGiamGia> getPage(Pageable pageable) {
        return dotGiamGiaRepository.findAll(pageable);
    }

    @Override
    public void addDotGiamGia(DotGiamGia dotGiamGia) {
        dotGiamGia.setNgayTao(LocalDateTime.now());
        dotGiamGia.setTrangThai(true);
        dotGiamGiaRepository.save(dotGiamGia);
    }

    @Override
    public DotGiamGia detailDotGiamGia(Long id) {
        return dotGiamGiaRepository.findById(id).orElse(null);
    }
}
