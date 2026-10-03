package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.MauSac;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.MauSacRepository;
import com.example.datn.service.MauSacService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MauSacServiceImpl implements MauSacService {

    private final MauSacRepository mauSacRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return mauSacRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenMau(), item.getMaMau(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        MauSac item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (mauSacRepository.existsByTenMauIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Màu sắc '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("MS%03d", mauSacRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        MauSac entity = new MauSac(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        MauSac saved = mauSacRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        MauSac entity = findById(id);
        String name = request.getTen().trim();

        if (mauSacRepository.existsByTenMauIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Màu sắc '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenMau(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        MauSac saved = mauSacRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        MauSac entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        MauSac saved = mauSacRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!mauSacRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Màu sắc với ID: " + id);
        }
        mauSacRepository.deleteById(id);
    }

    private MauSac findById(Long id) {
        return mauSacRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Màu sắc với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(MauSac item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaMau() != null ? item.getMaMau() : "")
                .ten(item.getTenMau())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
