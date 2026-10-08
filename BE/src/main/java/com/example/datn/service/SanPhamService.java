package com.example.datn.service;

import com.example.datn.dto.SanPhamRequest;
import com.example.datn.dto.SanPhamResponse;
import com.example.datn.dto.SmartProductSaveRequest;
import com.example.datn.dto.SmartProductSaveResponse;
import org.springframework.data.domain.Page;

public interface SanPhamService {
    Page<SanPhamResponse> getAll(int page, int size, String keyword, Long idThuongHieu, Long idLoaiGiay, Long idChatLieu, Long idKieuDang, Long idXuatXu, String doiTuong, Boolean trangThai);
    SanPhamResponse getById(Long id);
    SanPhamResponse create(SanPhamRequest request);
    SanPhamResponse update(Long id, SanPhamRequest request);
    SanPhamResponse toggleStatus(Long id);
    void delete(Long id);
    SmartProductSaveResponse smartSave(SmartProductSaveRequest request);
}

