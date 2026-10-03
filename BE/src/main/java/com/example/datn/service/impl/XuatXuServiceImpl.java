package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.XuatXu;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.XuatXuRepository;
import com.example.datn.service.XuatXuService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class XuatXuServiceImpl implements XuatXuService {

    private final XuatXuRepository xuatXuRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return xuatXuRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenXuatXu(), item.getMaXuatXu(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        XuatXu item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (xuatXuRepository.existsByTenXuatXuIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Xuất xứ '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("XX%03d", xuatXuRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        XuatXu entity = new XuatXu(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        XuatXu saved = xuatXuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        XuatXu entity = findById(id);
        String name = request.getTen().trim();

        if (xuatXuRepository.existsByTenXuatXuIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Xuất xứ '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenXuatXu(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        XuatXu saved = xuatXuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        XuatXu entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        XuatXu saved = xuatXuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!xuatXuRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Xuất xứ với ID: " + id);
        }
        xuatXuRepository.deleteById(id);
    }

    private XuatXu findById(Long id) {
        return xuatXuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Xuất xứ với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(XuatXu item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaXuatXu() != null ? item.getMaXuatXu() : "")
                .ten(item.getTenXuatXu())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
