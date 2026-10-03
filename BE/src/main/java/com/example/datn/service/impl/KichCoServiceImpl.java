package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.KichCo;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.KichCoRepository;
import com.example.datn.service.KichCoService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class KichCoServiceImpl implements KichCoService {

    private final KichCoRepository kichCoRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return kichCoRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenKichCo(), item.getMaKichCo(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        KichCo item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (kichCoRepository.existsByTenKichCoIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Kích cỡ '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("KC%03d", kichCoRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        KichCo entity = new KichCo(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        KichCo saved = kichCoRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        KichCo entity = findById(id);
        String name = request.getTen().trim();

        if (kichCoRepository.existsByTenKichCoIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Kích cỡ '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenKichCo(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        KichCo saved = kichCoRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        KichCo entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        KichCo saved = kichCoRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!kichCoRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Kích cỡ với ID: " + id);
        }
        kichCoRepository.deleteById(id);
    }

    private KichCo findById(Long id) {
        return kichCoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Kích cỡ với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(KichCo item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaKichCo() != null ? item.getMaKichCo() : "")
                .ten(item.getTenKichCo())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
