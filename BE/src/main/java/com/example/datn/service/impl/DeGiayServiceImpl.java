package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.DeGiay;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.DeGiayRepository;
import com.example.datn.service.DeGiayService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class DeGiayServiceImpl implements DeGiayService {

    private final DeGiayRepository deGiayRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return deGiayRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenDeGiay(), item.getMaDeGiay(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        DeGiay item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (deGiayRepository.existsByTenDeGiayIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Đế giày '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("DG%03d", deGiayRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        DeGiay entity = new DeGiay(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        DeGiay saved = deGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        DeGiay entity = findById(id);
        String name = request.getTen().trim();

        if (deGiayRepository.existsByTenDeGiayIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Đế giày '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenDeGiay(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        DeGiay saved = deGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        DeGiay entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        DeGiay saved = deGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!deGiayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Đế giày với ID: " + id);
        }
        deGiayRepository.deleteById(id);
    }

    private DeGiay findById(Long id) {
        return deGiayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Đế giày với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(DeGiay item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaDeGiay() != null ? item.getMaDeGiay() : "")
                .ten(item.getTenDeGiay())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
