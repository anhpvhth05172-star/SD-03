package com.example.datn.service;

import com.example.datn.dto.SanPham.SanPhamRequest;
import com.example.datn.dto.SanPham.SanPhamResponse;
import com.example.datn.dto.SanPham.SmartProductSaveRequest;
import com.example.datn.dto.SanPham.SmartProductSaveResponse;
import org.springframework.data.domain.Page;

public interface SanPhamService {
    Page<SanPhamResponse> getAll(int page, int size, String keyword, Long idThuongHieu, Long idLoaiGiay, Long idChatLieu, Long idKieuDang, Long idXuatXu, String doiTuong, Boolean trangThai);
    SanPhamResponse getById(Long id);
    SanPhamResponse create(SanPhamRequest request);
    SanPhamResponse update(Long id, SanPhamRequest request);
    SanPhamResponse toggleStatus(Long id);
    void delete(Long id);
    SmartProductSaveResponse smartSave(SmartProductSaveRequest request);
    byte[] downloadExcelTemplate();
    com.example.datn.dto.SanPham.ProductExcelImportResult importFromExcel(org.springframework.web.multipart.MultipartFile file);
}

