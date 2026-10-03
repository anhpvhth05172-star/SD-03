package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.ThanGiay;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.ThanGiayRepository;
import com.example.datn.service.ThanGiayService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ThanGiayServiceImpl implements ThanGiayService {

    private final ThanGiayRepository thanGiayRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return thanGiayRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenThanGiay(), item.getMaThanGiay(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        ThanGiay item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (thanGiayRepository.existsByTenThanGiayIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Thân giày '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("TG%03d", thanGiayRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        ThanGiay entity = new ThanGiay(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        ThanGiay saved = thanGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        ThanGiay entity = findById(id);
        String name = request.getTen().trim();

        if (thanGiayRepository.existsByTenThanGiayIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Thân giày '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenThanGiay(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        ThanGiay saved = thanGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        ThanGiay entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        ThanGiay saved = thanGiayRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!thanGiayRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Thân giày với ID: " + id);
        }
        thanGiayRepository.deleteById(id);
    }

    private ThanGiay findById(Long id) {
        return thanGiayRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Thân giày với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(ThanGiay item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaThanGiay() != null ? item.getMaThanGiay() : "")
                .ten(item.getTenThanGiay())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
