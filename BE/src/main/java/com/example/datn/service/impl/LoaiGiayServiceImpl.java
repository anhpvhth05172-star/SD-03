package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.LoaiGiay;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.LoaiGiayRepository;
import com.example.datn.service.LoaiGiayService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoaiGiayServiceImpl implements LoaiGiayService {

    private final LoaiGiayRepository loaiGiayRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return loaiGiayRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenLoaiGiay(), item.getMaLoaiGiay(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        LoaiGiay item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (loaiGiayRepository.existsByTenLoaiGiayIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Loại giày '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("LG%03d", loaiGiayRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        LoaiGiay entity = new LoaiGiay(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        LoaiGiay saved = loaiGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        LoaiGiay entity = findById(id);
        String name = request.getTen().trim();

        if (loaiGiayRepository.existsByTenLoaiGiayIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Loại giày '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenLoaiGiay(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        LoaiGiay saved = loaiGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        LoaiGiay entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        LoaiGiay saved = loaiGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!loaiGiayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Loại giày với ID: " + id);
        }
        loaiGiayRepository.deleteById(id);
    }

    private LoaiGiay findById(Long id) {
        return loaiGiayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Loại giày với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(LoaiGiay item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaLoaiGiay() != null ? item.getMaLoaiGiay() : "")
                .ten(item.getTenLoaiGiay())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
