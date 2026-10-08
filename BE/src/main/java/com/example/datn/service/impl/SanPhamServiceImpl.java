package com.example.datn.service.impl;

import com.example.datn.dto.SanPhamRequest;
import com.example.datn.dto.SanPhamResponse;
import com.example.datn.dto.SmartProductSaveRequest;
import com.example.datn.dto.SmartProductSaveResponse;
import com.example.datn.dto.SmartProductSaveRequest.BienTheItem;
import com.example.datn.dto.SmartProductSaveResponse.MergePreviewItem;
import com.example.datn.entity.ChatLieu;
import com.example.datn.entity.KieuDang;
import com.example.datn.entity.LoaiGiay;
import com.example.datn.entity.SanPham;
import com.example.datn.entity.ThuongHieu;
import com.example.datn.entity.XuatXu;
import com.example.datn.repository.ChatLieuRepository;
import com.example.datn.repository.KieuDangRepository;
import com.example.datn.repository.LoaiGiayRepository;
import com.example.datn.repository.SanPhamRepository;
import com.example.datn.repository.*;
import com.example.datn.service.SanPhamService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SanPhamServiceImpl implements SanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final ChiTietHoaDonRepository chiTietHoaDonRepository;
    private final XuatXuRepository xuatXuRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final KieuDangRepository kieuDangRepository;
    private final LoaiGiayRepository loaiGiayRepository;
    private final HinhAnhSanPhamRepository hinhAnhSanPhamRepository;
    private final MauSacRepository mauSacRepository;
    private final KichCoRepository kichCoRepository;
    private final ThanGiayRepository thanGiayRepository;
    private final DeGiayRepository deGiayRepository;

    @Override
    public Page<SanPhamResponse> getAll(int page, int size, String keyword, Long idThuongHieu, Long idLoaiGiay, Long idChatLieu, Long idKieuDang, Long idXuatXu, String doiTuong, Boolean trangThai) {
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
        Page<SanPham> sanPhamPage = sanPhamRepository.filterSanPhams(
                keyword != null && !keyword.trim().isEmpty() ? keyword.trim() : null,
                idThuongHieu,
                idLoaiGiay,
                idChatLieu,
                idKieuDang,
                idXuatXu,
                doiTuong != null && !doiTuong.trim().isEmpty() ? doiTuong.trim() : null,
                trangThai,
                pageable
        );
        return sanPhamPage.map(this::mapToResponse);
    }

    @Override
    public SanPhamResponse getById(Long id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        return mapToResponse(sanPham);
    }

    @Override
    @Transactional
    public SanPhamResponse create(SanPhamRequest request) {
        String name = request.getTenSanPham() != null ? request.getTenSanPham().trim() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (sanPhamRepository.existsByTenSanPhamIgnoreCase(name)) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại trong hệ thống");
        }

        XuatXu xuatXu = xuatXuRepository.findById(request.getIdXuatXu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Xuất xứ hợp lệ"));
        ThuongHieu thuongHieu = thuongHieuRepository.findById(request.getIdThuongHieu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Thương hiệu hợp lệ"));
        ChatLieu chatLieu = chatLieuRepository.findById(request.getIdChatLieu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Chất liệu hợp lệ"));
        KieuDang kieuDang = kieuDangRepository.findById(request.getIdKieuDang())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Kiểu dáng hợp lệ"));
        LoaiGiay loaiGiay = loaiGiayRepository.findById(request.getIdLoaiGiay())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Loại giày hợp lệ"));

        SanPham sanPham = new SanPham();
        sanPham.setTenSanPham(name);
        sanPham.setMaSanPham(generateProductCode());
        sanPham.setXuatXu(xuatXu);
        sanPham.setThuongHieu(thuongHieu);
        sanPham.setChatLieu(chatLieu);
        sanPham.setKieuDang(kieuDang);
        sanPham.setLoaiGiay(loaiGiay);
        sanPham.setDoiTuong(request.getDoiTuong());
        sanPham.setTinhNang(request.getTinhNang());
        sanPham.setMoTa(request.getMoTa());
        sanPham.setTrangThai(request.getTrangThai() != null ? request.getTrangThai() : true);
        sanPham.setNgayTao(LocalDateTime.now());
        sanPham.setNgayCapNhat(LocalDateTime.now());

        SanPham saved = sanPhamRepository.save(sanPham);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public SanPhamResponse update(Long id, SanPhamRequest request) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        String name = request.getTenSanPham() != null ? request.getTenSanPham().trim() : "";
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (sanPhamRepository.existsByTenSanPhamIgnoreCaseAndIdNot(name, id)) {
            throw new IllegalArgumentException("Tên sản phẩm đã tồn tại trong hệ thống");
        }

        XuatXu xuatXu = xuatXuRepository.findById(request.getIdXuatXu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Xuất xứ hợp lệ"));
        ThuongHieu thuongHieu = thuongHieuRepository.findById(request.getIdThuongHieu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Thương hiệu hợp lệ"));
        ChatLieu chatLieu = chatLieuRepository.findById(request.getIdChatLieu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Chất liệu hợp lệ"));
        KieuDang kieuDang = kieuDangRepository.findById(request.getIdKieuDang())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Kiểu dáng hợp lệ"));
        LoaiGiay loaiGiay = loaiGiayRepository.findById(request.getIdLoaiGiay())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Loại giày hợp lệ"));

        sanPham.setTenSanPham(name);
        sanPham.setXuatXu(xuatXu);
        sanPham.setThuongHieu(thuongHieu);
        sanPham.setChatLieu(chatLieu);
        sanPham.setKieuDang(kieuDang);
        sanPham.setLoaiGiay(loaiGiay);
        sanPham.setDoiTuong(request.getDoiTuong());
        sanPham.setTinhNang(request.getTinhNang());
        sanPham.setMoTa(request.getMoTa());
        if (request.getTrangThai() != null) {
            sanPham.setTrangThai(request.getTrangThai());
        }
        sanPham.setNgayCapNhat(LocalDateTime.now());

        SanPham updated = sanPhamRepository.save(sanPham);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public SanPhamResponse toggleStatus(Long id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));
        sanPham.setTrangThai(!sanPham.getTrangThai());
        sanPham.setNgayCapNhat(LocalDateTime.now());
        SanPham updated = sanPhamRepository.save(sanPham);
        return mapToResponse(updated);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        SanPham sanPham = sanPhamRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy sản phẩm với ID: " + id));

        // Kiểm tra xem sản phẩm đã phát sinh hóa đơn bán hàng chưa
        if (chiTietHoaDonRepository.existsBySanPhamChiTiet_SanPham_Id(id)) {
            throw new IllegalArgumentException("Không thể xóa sản phẩm đã có lịch sử hóa đơn hoặc giao dịch bán hàng!");
        }

        // Xóa ảnh của sản phẩm
        hinhAnhSanPhamRepository.deleteBySanPhamId(id);

        // Xóa tất cả biến thể chi tiết của sản phẩm trước
        sanPhamChiTietRepository.deleteBySanPhamId(id);

        // Xóa sản phẩm
        sanPhamRepository.delete(sanPham);
    }

    private String generateProductCode() {
        Optional<SanPham> latestOpt = sanPhamRepository.findFirstByOrderByIdDesc();
        long nextId = latestOpt.map(sp -> sp.getId() + 1).orElse(1L);
        String code = String.format("SP%03d", nextId);
        int count = 1;
        while (sanPhamRepository.existsByMaSanPhamIgnoreCase(code)) {
            code = String.format("SP%03d", nextId + count);
            count++;
        }
        return code;
    }

    private SanPhamResponse mapToResponse(SanPham sp) {
        List<com.example.datn.entity.SanPhamChiTiet> variants = sanPhamChiTietRepository.findBySanPhamId(sp.getId());
        int totalQty = variants.stream()
                .mapToInt(v -> v.getSoLuong() != null ? v.getSoLuong() : 0)
                .sum();
        java.math.BigDecimal minPrice = variants.stream()
                .map(com.example.datn.entity.SanPhamChiTiet::getGiaBan)
                .filter(java.util.Objects::nonNull)
                .min(java.math.BigDecimal::compareTo)
                .orElse(java.math.BigDecimal.ZERO);
        java.math.BigDecimal maxPrice = variants.stream()
                .map(com.example.datn.entity.SanPhamChiTiet::getGiaBan)
                .filter(java.util.Objects::nonNull)
                .max(java.math.BigDecimal::compareTo)
                .orElse(java.math.BigDecimal.ZERO);

        return SanPhamResponse.builder()
                .id(sp.getId())
                .maSanPham(sp.getMaSanPham())
                .tenSanPham(sp.getTenSanPham())
                .idXuatXu(sp.getXuatXu() != null ? sp.getXuatXu().getId() : null)
                .tenXuatXu(sp.getXuatXu() != null ? sp.getXuatXu().getTenXuatXu() : null)
                .idThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getId() : null)
                .tenThuongHieu(sp.getThuongHieu() != null ? sp.getThuongHieu().getTenThuongHieu() : null)
                .idChatLieu(sp.getChatLieu() != null ? sp.getChatLieu().getId() : null)
                .tenChatLieu(sp.getChatLieu() != null ? sp.getChatLieu().getTenChatLieu() : null)
                .idKieuDang(sp.getKieuDang() != null ? sp.getKieuDang().getId() : null)
                .tenKieuDang(sp.getKieuDang() != null ? sp.getKieuDang().getTenKieuDang() : null)
                .idLoaiGiay(sp.getLoaiGiay() != null ? sp.getLoaiGiay().getId() : null)
                .tenLoaiGiay(sp.getLoaiGiay() != null ? sp.getLoaiGiay().getTenLoaiGiay() : null)
                .doiTuong(sp.getDoiTuong())
                .tinhNang(sp.getTinhNang())
                .moTa(sp.getMoTa())
                .soLuong(totalQty)
                .giaBan(minPrice)
                .giaBanMin(minPrice)
                .giaBanMax(maxPrice)
                .trangThai(sp.getTrangThai())
                .ngayTao(sp.getNgayTao())
                .ngayCapNhat(sp.getNgayCapNhat())
                .build();
    }

    @Override
    @Transactional
    public SmartProductSaveResponse smartSave(SmartProductSaveRequest request) {
        String rawName = request.getTenSanPham() != null ? request.getTenSanPham().trim() : "";
        String normalizedName = rawName.replaceAll("\\s+", " ");
        if (normalizedName.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }

        XuatXu xuatXu = xuatXuRepository.findById(request.getIdXuatXu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Xuất xứ hợp lệ"));
        ThuongHieu thuongHieu = thuongHieuRepository.findById(request.getIdThuongHieu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Thương hiệu hợp lệ"));
        ChatLieu chatLieu = chatLieuRepository.findById(request.getIdChatLieu())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Chất liệu hợp lệ"));
        KieuDang kieuDang = kieuDangRepository.findById(request.getIdKieuDang())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Kiểu dáng hợp lệ"));
        LoaiGiay loaiGiay = loaiGiayRepository.findById(request.getIdLoaiGiay())
                .orElseThrow(() -> new IllegalArgumentException("Vui lòng chọn Loại giày hợp lệ"));

        // Validate hình ảnh (Magic Bytes, dung lượng tối đa 5MB)
        if (request.getBienThes() != null) {
            for (BienTheItem item : request.getBienThes()) {
                if (item.getHinhAnh() != null && !item.getHinhAnh().trim().isEmpty()) {
                    com.example.datn.util.ImageValidator.validateBase64Image(item.getHinhAnh());
                }
            }
        }

        Optional<SanPham> existingSpOpt = sanPhamRepository.findByTenSanPhamIgnoreCase(normalizedName);

        // Case 1: Sản phẩm đã tồn tại trong hệ thống
        if (existingSpOpt.isPresent()) {
            SanPham existingSp = existingSpOpt.get();

            // Nếu chưa xác nhận merge (confirmMerge == false) -> Trả về danh sách so sánh đối chiếu để FE hiện popup Confirm
            if (!Boolean.TRUE.equals(request.getConfirmMerge())) {
                List<MergePreviewItem> previewItems = new java.util.ArrayList<>();
                
                if (request.getBienThes() != null) {
                    for (BienTheItem item : request.getBienThes()) {
                        com.example.datn.entity.MauSac ms = mauSacRepository.findById(item.getIdMauSac()).orElse(null);
                        com.example.datn.entity.KichCo kc = kichCoRepository.findById(item.getIdKichCo()).orElse(null);

                        Optional<com.example.datn.entity.SanPhamChiTiet> spctOpt = 
                                sanPhamChiTietRepository.findBySanPhamIdAndMauSacIdAndKichCoId(existingSp.getId(), item.getIdMauSac(), item.getIdKichCo());

                        boolean isExisting = spctOpt.isPresent();
                        int currentStock = isExisting && spctOpt.get().getSoLuong() != null ? spctOpt.get().getSoLuong() : 0;
                        int addedStock = item.getSoLuong() != null ? item.getSoLuong() : 0;
                        int totalStock = currentStock + addedStock;
                        java.math.BigDecimal oldPrice = isExisting ? spctOpt.get().getGiaBan() : null;
                        java.math.BigDecimal newPrice = item.getGiaBan();

                        previewItems.add(MergePreviewItem.builder()
                                .idMauSac(item.getIdMauSac())
                                .tenMau(ms != null ? ms.getTenMau() : "Màu #" + item.getIdMauSac())
                                .maMau(ms != null ? ms.getMaMau() : null)
                                .idKichCo(item.getIdKichCo())
                                .tenKichCo(kc != null ? kc.getTenKichCo() : "Size #" + item.getIdKichCo())
                                .isExisting(isExisting)
                                .currentStock(currentStock)
                                .addedStock(addedStock)
                                .totalStock(totalStock)
                                .oldPrice(oldPrice)
                                .newPrice(newPrice)
                                .build());
                    }
                }

                return SmartProductSaveResponse.builder()
                        .status("NEED_CONFIRMATION")
                        .message("Sản phẩm '" + existingSp.getTenSanPham() + "' (Mã: " + existingSp.getMaSanPham() + ") đã tồn tại trong hệ thống. Bạn có muốn cập nhật thông tin và cộng dồn số lượng kho cho các biến thể không?")
                        .idSanPham(existingSp.getId())
                        .maSanPham(existingSp.getMaSanPham())
                        .tenSanPham(existingSp.getTenSanPham())
                        .previewItems(previewItems)
                        .build();
            }

            // Người dùng đã Confirm -> Thực hiện cập nhật thuộc tính cha & cộng dồn / thêm mới biến thể
            existingSp.setXuatXu(xuatXu);
            existingSp.setThuongHieu(thuongHieu);
            existingSp.setChatLieu(chatLieu);
            existingSp.setKieuDang(kieuDang);
            existingSp.setLoaiGiay(loaiGiay);
            existingSp.setDoiTuong(request.getDoiTuong());
            existingSp.setTinhNang(request.getTinhNang());
            existingSp.setMoTa(request.getMoTa());
            if (request.getTrangThai() != null) {
                existingSp.setTrangThai(request.getTrangThai());
            }
            existingSp.setNgayCapNhat(LocalDateTime.now());
            sanPhamRepository.save(existingSp);

            processSaveVariants(existingSp, request.getBienThes());

            return SmartProductSaveResponse.builder()
                    .status("SUCCESS")
                    .message("Đã cập nhật thông tin và cộng dồn số lượng kho thành công cho sản phẩm '" + existingSp.getTenSanPham() + "'")
                    .idSanPham(existingSp.getId())
                    .maSanPham(existingSp.getMaSanPham())
                    .tenSanPham(existingSp.getTenSanPham())
                    .build();
        }

        // Case 2: Sản phẩm hoàn toàn mới -> Tạo mới Sản phẩm cha & Tạo mới toàn bộ biến thể
        SanPham newSp = new SanPham();
        newSp.setTenSanPham(normalizedName);
        newSp.setMaSanPham(generateProductCode());
        newSp.setXuatXu(xuatXu);
        newSp.setThuongHieu(thuongHieu);
        newSp.setChatLieu(chatLieu);
        newSp.setKieuDang(kieuDang);
        newSp.setLoaiGiay(loaiGiay);
        newSp.setDoiTuong(request.getDoiTuong());
        newSp.setTinhNang(request.getTinhNang());
        newSp.setMoTa(request.getMoTa());
        newSp.setTrangThai(request.getTrangThai() != null ? request.getTrangThai() : true);
        newSp.setNgayTao(LocalDateTime.now());
        newSp.setNgayCapNhat(LocalDateTime.now());

        SanPham savedSp = sanPhamRepository.save(newSp);
        processSaveVariants(savedSp, request.getBienThes());

        return SmartProductSaveResponse.builder()
                .status("SUCCESS")
                .message("Thêm mới sản phẩm '" + savedSp.getTenSanPham() + "' thành công!")
                .idSanPham(savedSp.getId())
                .maSanPham(savedSp.getMaSanPham())
                .tenSanPham(savedSp.getTenSanPham())
                .build();
    }

    private void processSaveVariants(SanPham sp, List<BienTheItem> bienThes) {
        if (bienThes == null || bienThes.isEmpty()) return;

        com.example.datn.entity.ThanGiay defaultTg = thanGiayRepository.findAll().stream().findFirst().orElseGet(() -> {
            com.example.datn.entity.ThanGiay tg = new com.example.datn.entity.ThanGiay();
            tg.setTenThanGiay("Thân giày tiêu chuẩn");
            tg.setMaThanGiay("TG-DEFAULT");
            tg.setTrangThai(true);
            return thanGiayRepository.save(tg);
        });

        com.example.datn.entity.DeGiay defaultDg = deGiayRepository.findAll().stream().findFirst().orElseGet(() -> {
            com.example.datn.entity.DeGiay dg = new com.example.datn.entity.DeGiay();
            dg.setTenDeGiay("Đế giày cao su");
            dg.setMaDeGiay("DG-DEFAULT");
            dg.setTrangThai(true);
            return deGiayRepository.save(dg);
        });

        java.util.Set<String> savedImages = new java.util.HashSet<>();

        for (BienTheItem item : bienThes) {
            com.example.datn.entity.MauSac ms = mauSacRepository.findById(item.getIdMauSac())
                    .orElseThrow(() -> new IllegalArgumentException("Màu sắc ID " + item.getIdMauSac() + " không tồn tại"));
            com.example.datn.entity.KichCo kc = kichCoRepository.findById(item.getIdKichCo())
                    .orElseThrow(() -> new IllegalArgumentException("Kích cỡ ID " + item.getIdKichCo() + " không tồn tại"));

            Optional<com.example.datn.entity.SanPhamChiTiet> spctOpt = 
                    sanPhamChiTietRepository.findBySanPhamIdAndMauSacIdAndKichCoId(sp.getId(), item.getIdMauSac(), item.getIdKichCo());

            if (spctOpt.isPresent()) {
                // Biến thể đã tồn tại -> Cộng dồn số lượng & cập nhật giá mới
                com.example.datn.entity.SanPhamChiTiet spct = spctOpt.get();
                int currentQty = spct.getSoLuong() != null ? spct.getSoLuong() : 0;
                int addedQty = item.getSoLuong() != null ? item.getSoLuong() : 0;
                int updatedQty = currentQty + addedQty;
                spct.setSoLuong(updatedQty);
                if (item.getGiaBan() != null) {
                    spct.setGiaBan(item.getGiaBan());
                }
                if (updatedQty > 0) {
                    spct.setTrangThai(true);
                }
                sanPhamChiTietRepository.save(spct);
            } else {
                // Biến thể mới -> Tạo mới SanPhamChiTiet
                com.example.datn.entity.SanPhamChiTiet spct = new com.example.datn.entity.SanPhamChiTiet();
                spct.setSanPham(sp);
                spct.setMauSac(ms);
                spct.setKichCo(kc);
                spct.setThanGiay(defaultTg);
                spct.setDeGiay(defaultDg);

                String colorCode = ms.getMaMau() != null && !ms.getMaMau().trim().isEmpty() ? ms.getMaMau() : ("MS" + ms.getId());
                String sizeCode = kc.getTenKichCo() != null && !kc.getTenKichCo().trim().isEmpty() ? kc.getTenKichCo() : ("KC" + kc.getId());
                String maCode = sp.getMaSanPham() + "-" + colorCode + "-" + sizeCode;
                String finalMaCode = maCode;
                int counter = 1;
                while (sanPhamChiTietRepository.existsByMaChiTietSanPham(finalMaCode)) {
                    finalMaCode = maCode + "-" + counter++;
                }
                spct.setMaChiTietSanPham(finalMaCode);
                spct.setSoLuong(item.getSoLuong() != null ? item.getSoLuong() : 0);
                spct.setGiaBan(item.getGiaBan() != null ? item.getGiaBan() : java.math.BigDecimal.ZERO);
                spct.setTrangThai(item.getTrangThai() != null ? item.getTrangThai() : true);

                sanPhamChiTietRepository.save(spct);
            }

            // Lưu ảnh nếu có
            if (item.getHinhAnh() != null && !item.getHinhAnh().trim().isEmpty()) {
                String imgKey = sp.getId() + "_" + ms.getId();
                if (!savedImages.contains(imgKey)) {
                    savedImages.add(imgKey);
                    try {
                        List<com.example.datn.entity.HinhAnhSanPham> existingImages = hinhAnhSanPhamRepository.findBySanPhamId(sp.getId());
                        boolean imgExists = existingImages != null && existingImages.stream()
                                .anyMatch(img -> item.getHinhAnh().equals(img.getDuongDan()));
                        if (!imgExists) {
                            com.example.datn.entity.HinhAnhSanPham hasp = new com.example.datn.entity.HinhAnhSanPham();
                            hasp.setSanPham(sp);
                            hasp.setDuongDan(item.getHinhAnh());
                            hasp.setTenAnh("Ảnh màu " + ms.getTenMau());
                            hinhAnhSanPhamRepository.save(hasp);
                        }
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
