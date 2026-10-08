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
import java.time.LocalDateTime;
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
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final HinhAnhSanPhamRepository hinhAnhSanPhamRepository;

    @Override
    public Page<SanPhamChiTietResponse> getAll(int page, int size, String search, Long idSanPham, Long idMauSac, Long idKichCo, Boolean trangThai) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        String keyword = search != null && !search.trim().isEmpty() ? search.trim() : null;
        Page<SanPhamChiTiet> result = sanPhamChiTietRepository.filterChiTiet(keyword, idSanPham, idMauSac, idKichCo, trangThai, pageable);
        return result.map(spct -> mapToResponse(spct));
    }

    @Override
    public List<SanPhamChiTietResponse> getBySanPhamId(Long idSanPham) {
        return sanPhamChiTietRepository.findBySanPhamId(idSanPham)
                .stream()
                .map(spct -> mapToResponse(spct))
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
        if (requests == null || requests.isEmpty()) {
            throw new IllegalArgumentException("Danh sách biến thể không được để trống");
        }

        List<SanPhamChiTiet> list = new ArrayList<>();
        java.util.Set<String> savedImages = new java.util.HashSet<>();
        java.util.Set<String> batchCombinations = new java.util.HashSet<>();

        for (SanPhamChiTietRequest req : requests) {
            if (req.getIdSanPham() == null) {
                throw new IllegalArgumentException("Vui lòng chọn sản phẩm cho biến thể");
            }
            if (req.getIdMauSac() == null) {
                throw new IllegalArgumentException("Vui lòng chọn màu sắc cho biến thể");
            }
            if (req.getIdKichCo() == null) {
                throw new IllegalArgumentException("Vui lòng chọn kích cỡ cho biến thể");
            }
            if (req.getSoLuong() == null || req.getSoLuong() < 0 || req.getSoLuong() > 100000) {
                throw new IllegalArgumentException("Số lượng biến thể phải từ 0 đến 100,000 sản phẩm");
            }
            if (req.getGiaBan() == null || req.getGiaBan().compareTo(new BigDecimal("1000")) < 0 || req.getGiaBan().compareTo(new BigDecimal("1000000000")) > 0) {
                throw new IllegalArgumentException("Giá bán biến thể phải từ 1,000 VNĐ đến 1,000,000,000 VNĐ");
            }

            String comboKey = req.getIdSanPham() + "_" + req.getIdMauSac() + "_" + req.getIdKichCo();
            if (!batchCombinations.add(comboKey)) {
                throw new IllegalArgumentException("Có biến thể bị trùng lặp Màu sắc và Kích cỡ trong danh sách thêm!");
            }

            if (sanPhamChiTietRepository.existsBySanPhamIdAndMauSacIdAndKichCoId(req.getIdSanPham(), req.getIdMauSac(), req.getIdKichCo())) {
                MauSac ms = mauSacRepository.findById(req.getIdMauSac()).orElse(null);
                KichCo kc = kichCoRepository.findById(req.getIdKichCo()).orElse(null);
                String msName = ms != null && ms.getTenMau() != null ? ms.getTenMau() : "ID " + req.getIdMauSac();
                String kcName = kc != null && kc.getTenKichCo() != null ? kc.getTenKichCo() : "ID " + req.getIdKichCo();
                throw new IllegalArgumentException("Biến thể [Màu: " + msName + ", Kích cỡ: " + kcName + "] đã tồn tại trong sản phẩm!");
            }

            SanPham sp = sanPhamRepository.findById(req.getIdSanPham())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm với ID: " + req.getIdSanPham()));

            MauSac ms = mauSacRepository.findById(req.getIdMauSac())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy màu sắc với ID: " + req.getIdMauSac()));

            KichCo kc = kichCoRepository.findById(req.getIdKichCo())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kích cỡ với ID: " + req.getIdKichCo()));

            ThanGiay tg = req.getIdThanGiay() != null ? thanGiayRepository.findById(req.getIdThanGiay()).orElse(null) : null;
            DeGiay dg = req.getIdDeGiay() != null ? deGiayRepository.findById(req.getIdDeGiay()).orElse(null) : null;

            if (tg == null) {
                tg = thanGiayRepository.findAll().stream().findFirst().orElseGet(() -> {
                    ThanGiay defaultTg = new ThanGiay();
                    defaultTg.setTenThanGiay("Thân giày tiêu chuẩn");
                    defaultTg.setMaThanGiay("TG-DEFAULT");
                    defaultTg.setTrangThai(true);
                    return thanGiayRepository.save(defaultTg);
                });
            }
            if (dg == null) {
                dg = deGiayRepository.findAll().stream().findFirst().orElseGet(() -> {
                    DeGiay defaultDg = new DeGiay();
                    defaultDg.setTenDeGiay("Đế giày cao su");
                    defaultDg.setMaDeGiay("DG-DEFAULT");
                    defaultDg.setTrangThai(true);
                    return deGiayRepository.save(defaultDg);
                });
            }

            SanPhamChiTiet spct = new SanPhamChiTiet();
            spct.setSanPham(sp);
            spct.setKichCo(kc);
            spct.setMauSac(ms);
            spct.setThanGiay(tg);
            spct.setDeGiay(dg);

            String maCode = req.getMaChiTietSanPham();
            if (maCode == null || maCode.trim().isEmpty()) {
                String colorCode = ms.getMaMau() != null && !ms.getMaMau().trim().isEmpty() ? ms.getMaMau() : ("MS" + ms.getId());
                String sizeCode = kc.getTenKichCo() != null && !kc.getTenKichCo().trim().isEmpty() ? kc.getTenKichCo() : ("KC" + kc.getId());
                maCode = sp.getMaSanPham() + "-" + colorCode + "-" + sizeCode;
            }
            String finalMaCode = maCode;
            int counter = 1;
            while (sanPhamChiTietRepository.existsByMaChiTietSanPham(finalMaCode)) {
                finalMaCode = maCode + "-" + counter++;
            }
            spct.setMaChiTietSanPham(finalMaCode);
            spct.setSoLuong(req.getSoLuong());
            spct.setGiaBan(req.getGiaBan());
            spct.setTrangThai(req.getTrangThai() != null ? req.getTrangThai() : true);

            if (req.getHinhAnh() != null && !req.getHinhAnh().trim().isEmpty()) {
                String imgKey = sp.getId() + "_" + ms.getId();
                if (!savedImages.contains(imgKey)) {
                    savedImages.add(imgKey);
                    try {
                        List<HinhAnhSanPham> existingImages = hinhAnhSanPhamRepository.findBySanPhamId(sp.getId());
                        HinhAnhSanPham existingColorImg = (existingImages != null) ? existingImages.stream()
                                .filter(img -> img.getTenAnh() != null && (
                                        img.getTenAnh().contains("COLOR_" + ms.getId()) ||
                                        (ms.getTenMau() != null && !ms.getTenMau().trim().isEmpty() && img.getTenAnh().toLowerCase().contains(ms.getTenMau().toLowerCase()))
                                ))
                                .findFirst()
                                .orElse(null) : null;

                        if (existingColorImg != null) {
                            existingColorImg.setDuongDan(req.getHinhAnh().trim());
                            hinhAnhSanPhamRepository.save(existingColorImg);
                        } else {
                            HinhAnhSanPham hasp = new HinhAnhSanPham();
                            hasp.setSanPham(sp);
                            hasp.setTenAnh("COLOR_" + ms.getId() + " - " + (ms.getTenMau() != null ? ms.getTenMau() : ""));
                            hasp.setDuongDan(req.getHinhAnh().trim());
                            hinhAnhSanPhamRepository.save(hasp);
                        }
                    } catch (Exception ex) {
                        System.err.println("Lỗi lưu ảnh sản phẩm: " + ex.getMessage());
                    }
                }
            }

            list.add(spct);
        }
        List<SanPhamChiTiet> saved = sanPhamChiTietRepository.saveAll(list);
        return saved.stream().map(spct -> mapToResponse(spct)).collect(Collectors.toList());
    }

    @Override
    @Transactional
    public SanPhamChiTietResponse update(Long id, SanPhamChiTietRequest req) {
        SanPhamChiTiet spct = sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy biến thể với ID: " + id));

        if (req.getSoLuong() != null) {
            if (req.getSoLuong() < 0 || req.getSoLuong() > 100000) {
                throw new IllegalArgumentException("Số lượng tồn phải từ 0 đến 100,000");
            }
            spct.setSoLuong(req.getSoLuong());
        }

        if (req.getGiaBan() != null) {
            if (req.getGiaBan().compareTo(new BigDecimal("1000")) < 0 || req.getGiaBan().compareTo(new BigDecimal("1000000000")) > 0) {
                throw new IllegalArgumentException("Giá bán phải từ 1,000 VNĐ đến 1,000,000,000 VNĐ");
            }
            spct.setGiaBan(req.getGiaBan());
        }

        Long targetMauSacId = req.getIdMauSac() != null ? req.getIdMauSac() : (spct.getMauSac() != null ? spct.getMauSac().getId() : null);
        Long targetKichCoId = req.getIdKichCo() != null ? req.getIdKichCo() : (spct.getKichCo() != null ? spct.getKichCo().getId() : null);

        if (spct.getSanPham() != null && targetMauSacId != null && targetKichCoId != null) {
            boolean isDuplicate = sanPhamChiTietRepository.existsBySanPhamIdAndMauSacIdAndKichCoIdAndIdNot(
                    spct.getSanPham().getId(), targetMauSacId, targetKichCoId, id
            );
            if (isDuplicate) {
                MauSac ms = mauSacRepository.findById(targetMauSacId).orElse(null);
                KichCo kc = kichCoRepository.findById(targetKichCoId).orElse(null);
                String msName = ms != null && ms.getTenMau() != null ? ms.getTenMau() : "ID " + targetMauSacId;
                String kcName = kc != null && kc.getTenKichCo() != null ? kc.getTenKichCo() : "ID " + targetKichCoId;
                throw new IllegalArgumentException("Biến thể [Màu: " + msName + ", Kích cỡ: " + kcName + "] đã tồn tại cho sản phẩm này!");
            }
        }

        if (req.getIdKichCo() != null) {
            KichCo kc = kichCoRepository.findById(req.getIdKichCo())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kích cỡ hợp lệ"));
            spct.setKichCo(kc);
        }
        if (req.getIdMauSac() != null) {
            MauSac ms = mauSacRepository.findById(req.getIdMauSac())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy màu sắc hợp lệ"));
            spct.setMauSac(ms);
        }
        if (req.getIdThanGiay() != null) {
            ThanGiay tg = thanGiayRepository.findById(req.getIdThanGiay())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thân giày hợp lệ"));
            spct.setThanGiay(tg);
        }
        if (req.getIdDeGiay() != null) {
            DeGiay dg = deGiayRepository.findById(req.getIdDeGiay())
                    .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đế giày hợp lệ"));
            spct.setDeGiay(dg);
        }
        if (req.getTrangThai() != null) {
            spct.setTrangThai(req.getTrangThai());
        }
        if (req.getMaChiTietSanPham() != null && !req.getMaChiTietSanPham().trim().isEmpty()) {
            spct.setMaChiTietSanPham(req.getMaChiTietSanPham().trim());
        }

        if (req.getHinhAnh() != null && !req.getHinhAnh().trim().isEmpty() && spct.getSanPham() != null) {
            MauSac currentMs = spct.getMauSac();
            Long colorId = currentMs != null ? currentMs.getId() : null;
            String colorName = currentMs != null && currentMs.getTenMau() != null ? currentMs.getTenMau() : "";

            try {
                List<HinhAnhSanPham> existingImages = hinhAnhSanPhamRepository.findBySanPhamId(spct.getSanPham().getId());
                HinhAnhSanPham existingColorImg = (existingImages != null) ? existingImages.stream()
                        .filter(img -> img.getTenAnh() != null && colorId != null && (
                                img.getTenAnh().contains("COLOR_" + colorId) ||
                                (!colorName.isEmpty() && img.getTenAnh().toLowerCase().contains(colorName.toLowerCase()))
                        ))
                        .findFirst()
                        .orElse(null) : null;

                if (existingColorImg != null) {
                    existingColorImg.setDuongDan(req.getHinhAnh().trim());
                    hinhAnhSanPhamRepository.save(existingColorImg);
                } else {
                    HinhAnhSanPham hasp = new HinhAnhSanPham();
                    hasp.setSanPham(spct.getSanPham());
                    hasp.setTenAnh(colorId != null ? ("COLOR_" + colorId + " - " + colorName) : spct.getSanPham().getTenSanPham());
                    hasp.setDuongDan(req.getHinhAnh().trim());
                    hinhAnhSanPhamRepository.save(hasp);
                }
            } catch (Exception ex) {
                System.err.println("Lỗi cập nhật ảnh biến thể sản phẩm: " + ex.getMessage());
            }
        }

        SanPhamChiTiet saved = sanPhamChiTietRepository.save(spct);
        return mapToResponse(saved);
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
        SanPhamChiTiet item = sanPhamChiTietRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy biến thể với ID: " + id));

        if (chiTietHoaDonRepository.existsBySanPhamChiTietId(id)) {
            throw new IllegalArgumentException("Không thể xóa biến thể đã có lịch sử hóa đơn bán hàng!");
        }

        sanPhamChiTietRepository.delete(item);
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

        String imgUrl = null;
        if (spct.getSanPham() != null) {
            List<HinhAnhSanPham> images = hinhAnhSanPhamRepository.findBySanPhamId(spct.getSanPham().getId());
            if (images != null && !images.isEmpty()) {
                if (spct.getMauSac() != null) {
                    Long colorId = spct.getMauSac().getId();
                    String cName = spct.getMauSac().getTenMau();

                    imgUrl = images.stream()
                            .filter(img -> img.getTenAnh() != null && (
                                    (colorId != null && img.getTenAnh().contains("COLOR_" + colorId)) ||
                                    (cName != null && img.getTenAnh().toLowerCase().contains(cName.toLowerCase()))
                            ))
                            .map(HinhAnhSanPham::getDuongDan)
                            .findFirst()
                            .orElse(null);
                }

                if (imgUrl == null) {
                    imgUrl = images.get(0).getDuongDan();
                }
            }
        }

        int stockVal = spct.getSoLuong() != null ? spct.getSoLuong() : 0;
        String trangThaiTonKho;
        if (spct.getTrangThai() != null && !spct.getTrangThai()) {
            trangThaiTonKho = "Ngừng bán";
        } else if (stockVal == 0) {
            trangThaiTonKho = "Hết hàng";
        } else if (stockVal < 5) {
            trangThaiTonKho = "Sắp hết hàng";
        } else {
            trangThaiTonKho = "Đang bán";
        }

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
                .stock(stockVal)
                .price(spct.getGiaBan())
                .discount("-")
                .trangThai(spct.getTrangThai() != null ? spct.getTrangThai() : true)
                .trangThaiTonKho(trangThaiTonKho)
                .img(imgUrl)
                .ngayTao(spct.getNgayTao())
                .build();
    }
}
