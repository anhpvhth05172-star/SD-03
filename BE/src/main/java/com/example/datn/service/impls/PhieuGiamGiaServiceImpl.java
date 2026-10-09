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
        phieuGiamGia.setTrangThai(true);
        phieuGiamGia.setSoLuongDaSuDung(0);
        phieuGiamGia.setNgayTao(LocalDateTime.now());
        phieuGiamGiaRepository.save(phieuGiamGia);
    }

    @Override
    public PhieuGiamGia detail(Long id) {
        return phieuGiamGiaRepository.findById(id).orElse(null);
    }

    @Override
    public void update(PhieuGiamGia req) {
        PhieuGiamGia db = phieuGiamGiaRepository.findById(req.getId())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy phiếu giảm giá"));

        db.setMaPhieuGiamGia(req.getMaPhieuGiamGia());
        db.setTenPhieuGiamGia(req.getTenPhieuGiamGia());
        db.setLoaiGiamGia(req.getLoaiGiamGia());
        db.setGiaTriGiam(req.getGiaTriGiam());
        db.setGiamToiDa(req.getGiamToiDa());
        db.setHoaDonToiThieu(req.getHoaDonToiThieu());
        db.setSoLuong(req.getSoLuong());
        db.setNgayBatDau(req.getNgayBatDau());
        db.setNgayKetThuc(req.getNgayKetThuc());
        db.setTrangThai(req.getTrangThai());
        db.setMoTa(req.getMoTa());
        db.setNgayCapNhat(LocalDateTime.now());
        // soLuongDaSuDung, ngayTao: không đụng tới, giữ nguyên giá trị trong DB

        phieuGiamGiaRepository.save(db);
    }
}
