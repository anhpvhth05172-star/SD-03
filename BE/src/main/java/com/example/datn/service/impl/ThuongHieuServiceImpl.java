package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.ThuongHieu;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.ThuongHieuRepository;
import com.example.datn.service.ThuongHieuService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThuongHieuServiceImpl implements ThuongHieuService {

    private final ThuongHieuRepository thuongHieuRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return thuongHieuRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenThuongHieu(), item.getMaThuongHieu(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        ThuongHieu item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (thuongHieuRepository.existsByTenThuongHieuIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Thương hiệu '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("TH%03d", thuongHieuRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        ThuongHieu entity = new ThuongHieu(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        ThuongHieu saved = thuongHieuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        ThuongHieu entity = findById(id);
        String name = request.getTen().trim();

        if (thuongHieuRepository.existsByTenThuongHieuIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Thương hiệu '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenThuongHieu(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        ThuongHieu saved = thuongHieuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        ThuongHieu entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        ThuongHieu saved = thuongHieuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!thuongHieuRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Thương hiệu với ID: " + id);
        }
        thuongHieuRepository.deleteById(id);
    }

    private ThuongHieu findById(Long id) {
        return thuongHieuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thương hiệu với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(ThuongHieu item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaThuongHieu() != null ? item.getMaThuongHieu() : "")
                .ten(item.getTenThuongHieu())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
