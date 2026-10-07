package com.example.datn.service;

import com.example.datn.dto.SanPhamChiTietRequest;
import com.example.datn.dto.SanPhamChiTietResponse;
import org.springframework.data.domain.Page;

import java.util.List;

public interface SanPhamChiTietService {
    Page<SanPhamChiTietResponse> getAll(int page, int size, String search, Long idSanPham, Long idMauSac, Long idKichCo, Boolean trangThai);
    List<SanPhamChiTietResponse> getBySanPhamId(Long idSanPham);
    SanPhamChiTietResponse getById(Long id);
    List<SanPhamChiTietResponse> createBatch(List<SanPhamChiTietRequest> requests);
    SanPhamChiTietResponse update(Long id, SanPhamChiTietRequest request);
    SanPhamChiTietResponse toggleStatus(Long id);
    void delete(Long id);
}
