package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.KieuDang;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.KieuDangRepository;
import com.example.datn.service.KieuDangService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KieuDangServiceImpl implements KieuDangService {

    private final KieuDangRepository kieuDangRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return kieuDangRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenKieuDang(), item.getMaKieuDang(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        KieuDang item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (kieuDangRepository.existsByTenKieuDangIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Kiểu dáng '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("KD%03d", kieuDangRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        KieuDang entity = new KieuDang(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        KieuDang saved = kieuDangRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        KieuDang entity = findById(id);
        String name = request.getTen().trim();

        if (kieuDangRepository.existsByTenKieuDangIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Kiểu dáng '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenKieuDang(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        KieuDang saved = kieuDangRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        KieuDang entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        KieuDang saved = kieuDangRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!kieuDangRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Kiểu dáng với ID: " + id);
        }
        kieuDangRepository.deleteById(id);
    }

    private KieuDang findById(Long id) {
        return kieuDangRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Kiểu dáng với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(KieuDang item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaKieuDang() != null ? item.getMaKieuDang() : "")
                .ten(item.getTenKieuDang())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
