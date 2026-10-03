package com.example.datn.service.impl;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.entity.ChatLieu;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.ChatLieuRepository;
import com.example.datn.service.ChatLieuService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatLieuServiceImpl implements ChatLieuService {

    private final ChatLieuRepository chatLieuRepository;

    @Override
    @Transactional(readOnly = true)
    public List<AttributeResponse> getAll(String search, Boolean activeOnly) {
        return chatLieuRepository.findAll().stream()
                .filter(item -> matchesSearch(search, item.getTenChatLieu(), item.getMaChatLieu(), item.getMoTa()))
                .filter(item -> activeOnly == null || item.getTrangThai().equals(activeOnly))
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public AttributeResponse getById(Long id) {
        ChatLieu item = findById(id);
        return toResponse(item);
    }

    @Override
    @Transactional
    public AttributeResponse create(AttributeRequest request) {
        String name = request.getTen().trim();
        if (chatLieuRepository.existsByTenChatLieuIgnoreCase(name)) {
            throw new DuplicateRecordException("Tên Chất liệu '" + name + "' đã tồn tại trong hệ thống!");
        }

        String ma = request.getMa();
        if (ma == null || ma.trim().isEmpty()) {
            ma = String.format("CL%03d", chatLieuRepository.count() + 1);
        } else {
            ma = ma.trim().toUpperCase();
        }

        ChatLieu entity = new ChatLieu(null, ma, name, request.getMoTa() != null ? request.getMoTa().trim() : "", request.getTrangThai() != null ? request.getTrangThai() : true);
        ChatLieu saved = chatLieuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse update(Long id, AttributeRequest request) {
        ChatLieu entity = findById(id);
        String name = request.getTen().trim();

        if (chatLieuRepository.existsByTenChatLieuIgnoreCaseAndIdNot(name, id)) {
            throw new DuplicateRecordException("Tên Chất liệu '" + name + "' đã tồn tại ở bản ghi khác!");
        }

        entity.setTenChatLieu(name);
        if (request.getMoTa() != null) {
            entity.setMoTa(request.getMoTa().trim());
        }
        if (request.getTrangThai() != null) {
            entity.setTrangThai(request.getTrangThai());
        }

        ChatLieu saved = chatLieuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public AttributeResponse toggleStatus(Long id) {
        ChatLieu entity = findById(id);
        entity.setTrangThai(!Boolean.TRUE.equals(entity.getTrangThai()));
        ChatLieu saved = chatLieuRepository.save(entity);
        return toResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!chatLieuRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy Chất liệu với ID: " + id);
        }
        chatLieuRepository.deleteById(id);
    }

    private ChatLieu findById(Long id) {
        return chatLieuRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy Chất liệu với ID: " + id));
    }

    private boolean matchesSearch(String search, String name, String code, String desc) {
        if (search == null || search.trim().isEmpty()) return true;
        String q = search.trim().toLowerCase();
        boolean matchName = name != null && name.toLowerCase().contains(q);
        boolean matchCode = code != null && code.toLowerCase().contains(q);
        boolean matchDesc = desc != null && desc.toLowerCase().contains(q);
        return matchName || matchCode || matchDesc;
    }

    private AttributeResponse toResponse(ChatLieu item) {
        boolean active = Boolean.TRUE.equals(item.getTrangThai());
        return AttributeResponse.builder()
                .id(item.getId())
                .ma(item.getMaChatLieu() != null ? item.getMaChatLieu() : "")
                .ten(item.getTenChatLieu())
                .moTa(item.getMoTa() != null ? item.getMoTa() : "")
                .trangThai(active ? "Hoạt động" : "Ngừng hoạt động")
                .status(active)
                .build();
    }
}
