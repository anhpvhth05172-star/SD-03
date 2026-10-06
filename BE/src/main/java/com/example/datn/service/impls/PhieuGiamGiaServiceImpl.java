package com.example.datn.service.impls;

import com.example.datn.dto.PhieuGiamGiaDTO;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.service.PhieuGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
public class PhieuGiamGiaServiceImpl implements PhieuGiamGiaService {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Override
    public Page<PhieuGiamGia> getPageList(Pageable pageable) {
        return phieuGiamGiaRepository.findAll(pageable);
    }

    @Override
    public void addPhieuGiamGia(PhieuGiamGia phieuGiamGia) {
        LocalDateTime now = LocalDateTime.now(ZoneId.of("Asia/Ho_Chi_Minh"));
        phieuGiamGia.setTrangThai(true);
        phieuGiamGia.setSoLuongDaSuDung(0);
        phieuGiamGia.setNgayTao(now);
        phieuGiamGia.setNgayCapNhat(now);
        phieuGiamGiaRepository.save(phieuGiamGia);
    }

    @Override
    public PhieuGiamGia detail(Long id) {
        return phieuGiamGiaRepository.findById(id).orElse(null);
    }
}
