package com.example.datn.service.impl;

import com.example.datn.dto.SanPhamChiTietRequest;
import com.example.datn.dto.SanPhamChiTietResponse;
import com.example.datn.entity.*;
import com.example.datn.repository.*;
import com.example.datn.service.SanPhamChiTietService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SanPhamChiTietServiceImpl implements SanPhamChiTietService {

    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final SanPhamRepository sanPhamRepository;
    private final MauSacRepository mauSacRepository;
    private final KichCoRepository kichCoRepository;
    private final ThanGiayRepository thanGiayRepository;
    private final DeGiayRepository deGiayRepository;

    @Override
    public Page<SanPhamChiTietResponse> getAll(int page, int size, String search, Long idSanPham, Long idMauSac, Long idKichCo, Boolean trangThai) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String keyword = search != null && !search.trim().isEmpty() ? search.trim() : null;
        Page<SanPhamChiTiet> result = sanPhamChiTietRepository.filterChiTiet(keyword, idSanPham, idMauSac, idKichCo, trangThai, pageable);
        return result.map(this::mapToResponse);
    }

    @Override
    public List<SanPhamChiTietResponse> getBySanPhamId(Long idSanPham) {
        return sanPhamChiTietRepository.findBySanPhamId(idSanPham)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public SanPhamChiTietResponse getById(Long id) {
        SanPhamChiTiet item = sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể với ID: " + id));
        return mapToResponse(item);
    }

    @Override
    @Transactional
    public List<SanPhamChiTietResponse> createBatch(List<SanPhamChiTietRequest> requests) {
        List<SanPhamChiTiet> list = new ArrayList<>();
        for (SanPhamChiTietRequest req : requests) {
            SanPham sp = sanPhamRepository.findById(req.getIdSanPham())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + req.getIdSanPham()));

            KichCo kc = req.getIdKichCo() != null ? kichCoRepository.findById(req.getIdKichCo()).orElse(null) : null;
            MauSac ms = req.getIdMauSac() != null ? mauSacRepository.findById(req.getIdMauSac()).orElse(null) : null;
            ThanGiay tg = req.getIdThanGiay() != null ? thanGiayRepository.findById(req.getIdThanGiay()).orElse(null) : null;
            DeGiay dg = req.getIdDeGiay() != null ? deGiayRepository.findById(req.getIdDeGiay()).orElse(null) : null;

            if (kc == null) {
                kc = kichCoRepository.findAll().stream().findFirst().orElse(null);
            }
            if (ms == null) {
                ms = mauSacRepository.findAll().stream().findFirst().orElse(null);
            }
            if (tg == null) {
                tg = thanGiayRepository.findAll().stream().findFirst().orElse(null);
            }
            if (dg == null) {
                dg = deGiayRepository.findAll().stream().findFirst().orElse(null);
            }

            SanPhamChiTiet spct = new SanPhamChiTiet();
            spct.setSanPham(sp);
            spct.setKichCo(kc);
            spct.setMauSac(ms);
            spct.setThanGiay(tg);
            spct.setDeGiay(dg);

            String maCode = req.getMaChiTietSanPham();
            if (maCode == null || maCode.trim().isEmpty()) {
                maCode = sp.getMaSanPham() + "-" + (ms != null ? ms.getMaMau() : "MS") + "-" + (kc != null ? kc.getTenKichCo() : "KC");
            }
            spct.setMaChiTietSanPham(maCode);
            spct.setSoLuong(req.getSoLuong() != null ? req.getSoLuong() : 10);
            spct.setGiaBan(req.getGiaBan() != null ? req.getGiaBan() : new BigDecimal("3500000"));
            spct.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : true);

            list.add(spct);
        }
        List<SanPhamChiTiet> saved = sanPhamChiTietRepository.saveAll(list);
        return saved.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SanPhamChiTietResponse toggleStatus(Long id) {
        SanPhamChiTiet item = sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể với ID: " + id));
        item.setTrangThai(item.getTrangThai() == null || !item.getTrangThai());
        SanPhamChiTiet saved = sanPhamChiTietRepository.save(item);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        if (!sanPhamChiTietRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy biến thể với ID: " + id);
        }
        sanPhamChiTietRepository.deleteById(id);
    }

    private SanPhamChiTietResponse mapToResponse(SanPhamChiTiet spct) {
        String colorHex = "#64748b";
        String colorName = "Mặc định";
        if (spct.getMauSac() != null) {
            colorName = spct.getMauSac().getTenMau() != null ? spct.getMauSac().getTenMau() : "Mặc định";
            if (spct.getMauSac().getMoTa() != null && spct.getMauSac().getMoTa().startsWith("#")) {
                colorHex = spct.getMauSac().getMoTa();
            }
        }

        String sizeName = "Standard";
        if (spct.getKichCo() != null) {
            sizeName = spct.getKichCo().getTenKichCo() != null ? spct.getKichCo().getTenKichCo() : "Standard";
        }

        String defaultImg = "https://images.unsplash.com/photo-1542291026-7eec264c27ff?w=120&auto=format&fit=crop&q=60";

        return SanPhamChiTietResponse.builder()
                .id(spct.getId())
                .idSanPham(spct.getSanPham() != null ? spct.getSanPham().getId() : null)
                .maSp(spct.getSanPham() != null ? spct.getSanPham().getMaSanPham() : "SP001")
                .tenSp(spct.getSanPham() != null ? spct.getSanPham().getTenSanPham() : "Sản phẩm")
                .maCtsp(spct.getMaChiTietSanPham() != null ? spct.getMaChiTietSanPham() : ("CTSP" + spct.getId()))
                .idMauSac(spct.getMauSac() != null ? spct.getMauSac().getId() : null)
                .color(colorName)
                .colorHex(colorHex)
                .idKichCo(spct.getKichCo() != null ? spct.getKichCo().getId() : null)
                .size(sizeName)
                .stock(spct.getSoLuong() != null ? spct.getSoLuong() : 0)
                .price(spct.getGiaBan())
                .discount("-")
                .trangThai(spct.getTrangThai() != null ? spct.getTrangThai() : true)
                .img(defaultImg)
                .ngayTao(spct.getNgayTao())
                .build();
    }
}
