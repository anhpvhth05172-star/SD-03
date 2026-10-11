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

    @Override
    public void updateDotGiamGia(DotGiamGia dotGiamGia) {
        if (dotGiamGia == null || dotGiamGia.getId() == null) {
            throw new IllegalArgumentException("Thiếu thông tin đợt giảm giá cần cập nhật");
        }
        if (!dotGiamGiaRepository.existsById(dotGiamGia.getId())) {
            throw new IllegalArgumentException("Không tìm thấy đợt giảm giá");
        }
        String ma = dotGiamGia.getMaDotGiamGia() == null ? "" : dotGiamGia.getMaDotGiamGia().trim();
        String ten = dotGiamGia.getTenDotGiamGia() == null ? "" : dotGiamGia.getTenDotGiamGia().trim();
        if (ma.isEmpty() || ma.length() > 50) {
            throw new IllegalArgumentException("Mã đợt giảm giá không được để trống và tối đa 50 ký tự");
        }
        if (dotGiamGiaRepository.existsByMaDotGiamGiaAndIdNot(ma, dotGiamGia.getId())) {
            throw new IllegalArgumentException("Mã đợt giảm giá đã tồn tại");
        }
        if (ten.isEmpty() || ten.length() > 200) {
            throw new IllegalArgumentException("Tên đợt giảm giá không được để trống và tối đa 200 ký tự");
        }
        if (dotGiamGia.getPhanTramGiam() == null
            || dotGiamGia.getPhanTramGiam().signum() <= 0
            || dotGiamGia.getPhanTramGiam().compareTo(java.math.BigDecimal.valueOf(100)) > 0) {
            throw new IllegalArgumentException("Phần trăm giảm phải lớn hơn 0 và tối đa 100");
        }
        if (dotGiamGia.getNgayBatDau() == null || dotGiamGia.getNgayKetThuc() == null
            || !dotGiamGia.getNgayKetThuc().isAfter(dotGiamGia.getNgayBatDau())) {
            throw new IllegalArgumentException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        if (dotGiamGia.getTrangThai() == null) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái");
        }
        if (dotGiamGia.getMoTa() != null && dotGiamGia.getMoTa().length() > 500) {
            throw new IllegalArgumentException("Mô tả không được vượt quá 500 ký tự");
        }
        dotGiamGia.setMaDotGiamGia(ma);
        dotGiamGia.setTenDotGiamGia(ten);
        dotGiamGiaRepository.save(dotGiamGia);
    }

    @Override
    public Page<DotGiamGia> searching(String ten, Boolean trangThai, LocalDateTime from, LocalDateTime to, Pageable pageable) {
        return dotGiamGiaRepository.search(ten, trangThai, from, to, pageable);
    }
}
