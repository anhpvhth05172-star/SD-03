package com.example.datn.service;

import com.example.datn.dto.ThuocTinh.AttributeRequest;
import com.example.datn.dto.ThuocTinh.AttributeResponse;
import java.util.List;

public interface AttributeService {

    List<AttributeResponse> getAll(String search, Boolean activeOnly);

    AttributeResponse getById(Long id);

    AttributeResponse create(AttributeRequest request);

    AttributeResponse update(Long id, AttributeRequest request);

    AttributeResponse toggleStatus(Long id);

    void delete(Long id);
}
