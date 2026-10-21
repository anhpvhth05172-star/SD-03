package com.example.datn.service.impl;

import com.example.datn.dto.SanPham.SanPhamRequest;
import com.example.datn.dto.SanPham.SanPhamResponse;
import com.example.datn.dto.SanPham.SmartProductSaveRequest;
import com.example.datn.dto.SanPham.SmartProductSaveResponse;
import com.example.datn.dto.SanPham.SmartProductSaveRequest.BienTheItem;
import com.example.datn.dto.SanPham.SmartProductSaveResponse.MergePreviewItem;
import com.example.datn.entity.ChatLieu;
import com.example.datn.entity.KieuDang;
import com.example.datn.entity.LoaiGiay;
import com.example.datn.entity.SanPham;
import com.example.datn.entity.ThuongHieu;
import com.example.datn.entity.XuatXu;
import com.example.datn.entity.MauSac;
import com.example.datn.entity.KichCo;
import com.example.datn.repository.ChatLieuRepository;
import com.example.datn.repository.KieuDangRepository;
import com.example.datn.repository.LoaiGiayRepository;
import com.example.datn.repository.SanPhamRepository;
import com.example.datn.dto.DotGiamGia.GiaKhuyenMai;
import com.example.datn.repository.*;
import com.example.datn.service.SanPhamService;
import com.example.datn.service.impls.GiaService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    private final GiaService giaService;

    /** Tiêu đề mẫu của tệp import sản phẩm (14 cột, đọc theo đúng thứ tự). */
    private static final String[] TIEU_DE_MAU_SANPHAM = {
        "STT", "Tên sản phẩm (*)", "Thương hiệu (*)", "Loại giày (*)",
        "Chất liệu (*)", "Kiểu dáng (*)", "Xuất xứ (*)", "Đối tượng",
        "Màu sắc (*)", "Kích cỡ (*)", "Số lượng (*)", "Giá bán (VNĐ) (*)",
        "Tính năng", "Mô tả"
    };

    /** Chuỗi chuẩn hóa (không dấu, viết thường) mà mỗi cột tiêu đề phải bắt đầu bằng. */
    private static final String[] KY_MON_TIEU_DE_SANPHAM = {
        "stt", "ten san pham", "thuong hieu", "loai giay", "chat lieu",
        "kieu dang", "xuat xu", "doi tuong", "mau sac", "kich co",
        "so luong", "gia ban", "tinh nang", "mo ta"
    };

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

        if (chiTietHoaDonRepository.existsBySanPhamChiTiet_SanPham_Id(id)) {
            throw new IllegalArgumentException("Không thể xóa sản phẩm đã có lịch sử hóa đơn hoặc giao dịch bán hàng!");
        }

        hinhAnhSanPhamRepository.deleteBySanPhamId(id);

        sanPhamChiTietRepository.deleteBySanPhamId(id);

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

        Map<Long, GiaKhuyenMai> giaMap = giaService.tinhGia(variants);
        java.math.BigDecimal maxPhanTram = giaMap.values().stream()
                .map(GiaKhuyenMai::phanTramGiam)
                .filter(java.util.Objects::nonNull)
                .max(java.math.BigDecimal::compareTo)
                .orElse(null);

        String discountStr = "-";
        if (maxPhanTram != null && maxPhanTram.compareTo(java.math.BigDecimal.ZERO) > 0) {
            discountStr = "-" + maxPhanTram.stripTrailingZeros().toPlainString() + "%";
        }

        java.math.BigDecimal minGiaSauGiam = giaMap.values().stream()
                .map(GiaKhuyenMai::giaSauGiam)
                .filter(java.util.Objects::nonNull)
                .min(java.math.BigDecimal::compareTo)
                .orElse(minPrice);
        java.math.BigDecimal maxGiaSauGiam = giaMap.values().stream()
                .map(GiaKhuyenMai::giaSauGiam)
                .filter(java.util.Objects::nonNull)
                .max(java.math.BigDecimal::compareTo)
                .orElse(maxPrice);

        String hinhAnhDaiDien = null;
        try {
            List<com.example.datn.entity.HinhAnhSanPham> images = hinhAnhSanPhamRepository.findBySanPhamId(sp.getId());
            if (images != null && !images.isEmpty()) {
                hinhAnhDaiDien = images.get(0).getDuongDan();
            }
        } catch (Exception ignored) {
        }

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
                .discount(discountStr)
                .giaSauGiamMin(minGiaSauGiam)
                .giaSauGiamMax(maxGiaSauGiam)
                .hinhAnhDaiDien(hinhAnhDaiDien)
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

    @Override
    public byte[] downloadExcelTemplate() {
        try (org.apache.poi.xssf.usermodel.XSSFWorkbook workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook();
             java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream()) {

            org.apache.poi.ss.usermodel.Sheet sheet = workbook.createSheet("ImportSanPham");

            // Header Style
            org.apache.poi.ss.usermodel.CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(org.apache.poi.ss.usermodel.IndexedColors.WHITE.getIndex());
            headerFont.setFontHeightInPoints((short) 11);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(org.apache.poi.ss.usermodel.IndexedColors.SEA_GREEN.getIndex());
            headerStyle.setFillPattern(org.apache.poi.ss.usermodel.FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(org.apache.poi.ss.usermodel.HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            headerStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            headerStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            // Data Style
            org.apache.poi.ss.usermodel.CellStyle dataStyle = workbook.createCellStyle();
            dataStyle.setBorderBottom(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderTop(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderLeft(org.apache.poi.ss.usermodel.BorderStyle.THIN);
            dataStyle.setBorderRight(org.apache.poi.ss.usermodel.BorderStyle.THIN);

            String[] headers = {
                "STT", "Tên sản phẩm (*)", "Thương hiệu (*)", "Loại giày (*)",
                "Chất liệu (*)", "Kiểu dáng (*)", "Xuất xứ (*)", "Đối tượng",
                "Màu sắc (*)", "Kích cỡ (*)", "Số lượng (*)", "Giá bán (VNĐ) (*)",
                "Tính năng", "Mô tả"
            };

            org.apache.poi.ss.usermodel.Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                org.apache.poi.ss.usermodel.Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Dữ liệu mẫu
            String[][] sampleData = {
                {"1", "Giày Nike Air Force 1 07", "Nike", "Sneaker", "Da", "Cổ thấp", "Việt Nam", "Nam", "Trắng", "40", "50", "2600000", "Đệm Air êm ái", "Giày thể thao kinh điển phù hợp mọi phong cách"},
                {"2", "Giày Nike Air Force 1 07", "Nike", "Sneaker", "Da", "Cổ thấp", "Việt Nam", "Nam", "Trắng", "41", "40", "2600000", "Đệm Air êm ái", "Giày thể thao kinh điển phù hợp mọi phong cách"},
                {"3", "Giày Nike Air Force 1 07", "Nike", "Sneaker", "Da", "Cổ thấp", "Việt Nam", "Nam", "Đen", "40", "30", "2600000", "Đệm Air êm ái", "Giày thể thao kinh điển màu đen mạnh mẽ"},
                {"4", "Giày Adidas Ultraboost Light", "Adidas", "Giày chạy bộ", "Vải dệt", "Cổ thấp", "Việt Nam", "Unisex", "Trắng", "42", "25", "3500000", "Đế đệm Boost siêu nhẹ", "Giày chạy bộ chuyên nghiệp siêu êm ái"},
                {"5", "Giày Puma Suede Classic", "Puma", "Giày thời trang", "Da lộn", "Cổ thấp", "Việt Nam", "Nam", "Đỏ", "41", "20", "1900000", "Phong cách retro", "Chất liệu da lộn cao cấp"}
            };

            for (int r = 0; r < sampleData.length; r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.createRow(r + 1);
                row.setHeightInPoints(22);
                for (int c = 0; c < sampleData[r].length; c++) {
                    org.apache.poi.ss.usermodel.Cell cell = row.createCell(c);
                    cell.setCellValue(sampleData[r][c]);
                    cell.setCellStyle(dataStyle);
                }
            }

            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
                sheet.setColumnWidth(i, Math.max(sheet.getColumnWidth(i) + 1200, 3500));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Không thể tạo file mẫu Excel: " + e.getMessage(), e);
        }
    }

    @Override
    @Transactional
    public com.example.datn.dto.SanPham.ProductExcelImportResult importFromExcel(org.springframework.web.multipart.MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Vui lòng tải lên tệp Excel hợp lệ");
        }
        String fileName = file.getOriginalFilename() != null ? file.getOriginalFilename().toLowerCase() : "";
        if (!fileName.endsWith(".xlsx") && !fileName.endsWith(".xls")) {
            throw new IllegalArgumentException("Hệ thống chỉ chấp nhận định dạng Excel (.xlsx, .xls)");
        }

        com.example.datn.dto.SanPham.ProductExcelImportResult result = com.example.datn.dto.SanPham.ProductExcelImportResult.builder().build();

        try (org.apache.poi.ss.usermodel.Workbook workbook = org.apache.poi.ss.usermodel.WorkbookFactory.create(file.getInputStream())) {
            org.apache.poi.ss.usermodel.Sheet sheet = workbook.getSheetAt(0);
            int lastRowNum = sheet.getLastRowNum();
            if (lastRowNum < 1) {
                throw new IllegalArgumentException("File Excel không có dữ liệu để import");
            }

            // Hàng tiêu đề phải đúng mẫu 14 cột theo đúng thứ tự — sai/thiếu thì báo rõ
            // thay vì đọc nhầm dữ liệu theo vị trí cột.
            com.example.datn.util.ExcelTieuDeUtil.kiemTraTieuDe(
                sheet, new org.apache.poi.ss.usermodel.DataFormatter(),
                KY_MON_TIEU_DE_SANPHAM, TIEU_DE_MAU_SANPHAM);

            // Group rows by Normalized Product Name
            java.util.Map<String, List<ExcelRowData>> groupedProducts = new java.util.LinkedHashMap<>();

            for (int r = 1; r <= lastRowNum; r++) {
                org.apache.poi.ss.usermodel.Row row = sheet.getRow(r);
                if (row == null) continue;

                result.setTongSoDong(result.getTongSoDong() + 1);

                String tenSp = getCellValueAsString(row.getCell(1));
                if (tenSp.isEmpty()) {
                    // Dòng trống bỏ qua
                    result.setTongSoDong(result.getTongSoDong() - 1);
                    continue;
                }

                String thuongHieu = getCellValueAsString(row.getCell(2));
                String loaiGiay = getCellValueAsString(row.getCell(3));
                String chatLieu = getCellValueAsString(row.getCell(4));
                String kieuDang = getCellValueAsString(row.getCell(5));
                String xuatXu = getCellValueAsString(row.getCell(6));
                String doiTuong = getCellValueAsString(row.getCell(7));
                String mauSac = getCellValueAsString(row.getCell(8));
                String kichCo = getCellValueAsString(row.getCell(9));
                String soLuongStr = getCellValueAsString(row.getCell(10));
                String giaBanStr = getCellValueAsString(row.getCell(11));
                String tinhNang = getCellValueAsString(row.getCell(12));
                String moTa = getCellValueAsString(row.getCell(13));

                // Validate dòng
                int rowNumber = r + 1;
                if (mauSac.isEmpty()) {
                    result.setSoDongThatBai(result.getSoDongThatBai() + 1);
                    result.getDanhSachLoi().add("Dòng " + rowNumber + ": Màu sắc không được để trống");
                    continue;
                }
                if (kichCo.isEmpty()) {
                    result.setSoDongThatBai(result.getSoDongThatBai() + 1);
                    result.getDanhSachLoi().add("Dòng " + rowNumber + ": Kích cỡ không được để trống");
                    continue;
                }

                int soLuong = 0;
                try {
                    soLuong = Integer.parseInt(soLuongStr.replaceAll("[^0-9]", ""));
                    if (soLuong < 0) throw new NumberFormatException();
                } catch (Exception ex) {
                    result.setSoDongThatBai(result.getSoDongThatBai() + 1);
                    result.getDanhSachLoi().add("Dòng " + rowNumber + ": Số lượng '" + soLuongStr + "' không hợp lệ (phải là số nguyên >= 0)");
                    continue;
                }

                java.math.BigDecimal giaBan;
                try {
                    String cleanPrice = giaBanStr.replaceAll("[^0-9.]", "");
                    giaBan = new java.math.BigDecimal(cleanPrice);
                    if (giaBan.compareTo(java.math.BigDecimal.ZERO) <= 0) throw new NumberFormatException();
                } catch (Exception ex) {
                    result.setSoDongThatBai(result.getSoDongThatBai() + 1);
                    result.getDanhSachLoi().add("Dòng " + rowNumber + ": Giá bán '" + giaBanStr + "' không hợp lệ (phải > 0 VNĐ)");
                    continue;
                }

                ExcelRowData rowData = new ExcelRowData();
                rowData.rowNumber = rowNumber;
                rowData.tenSp = tenSp;
                rowData.thuongHieu = thuongHieu;
                rowData.loaiGiay = loaiGiay;
                rowData.chatLieu = chatLieu;
                rowData.kieuDang = kieuDang;
                rowData.xuatXu = xuatXu;
                rowData.doiTuong = doiTuong.isEmpty() ? "Nam" : doiTuong;
                rowData.mauSac = mauSac;
                rowData.kichCo = kichCo;
                rowData.soLuong = soLuong;
                rowData.giaBan = giaBan;
                rowData.tinhNang = tinhNang;
                rowData.moTa = moTa;

                String groupKey = tenSp.trim().toLowerCase();
                groupedProducts.computeIfAbsent(groupKey, k -> new java.util.ArrayList<>()).add(rowData);
            }

            // Xử lý lưu từng nhóm sản phẩm
            for (List<ExcelRowData> rows : groupedProducts.values()) {
                if (rows.isEmpty()) continue;
                ExcelRowData firstRow = rows.get(0);

                ThuongHieu th = findOrCreateThuongHieu(firstRow.thuongHieu);
                LoaiGiay lg = findOrCreateLoaiGiay(firstRow.loaiGiay);
                ChatLieu cl = findOrCreateChatLieu(firstRow.chatLieu);
                KieuDang kd = findOrCreateKieuDang(firstRow.kieuDang);
                XuatXu xx = findOrCreateXuatXu(firstRow.xuatXu);

                Optional<SanPham> existingSpOpt = sanPhamRepository.findByTenSanPhamIgnoreCase(firstRow.tenSp.trim());
                SanPham sp;
                if (existingSpOpt.isPresent()) {
                    sp = existingSpOpt.get();
                    sp.setThuongHieu(th);
                    sp.setLoaiGiay(lg);
                    sp.setChatLieu(cl);
                    sp.setKieuDang(kd);
                    sp.setXuatXu(xx);
                    if (firstRow.doiTuong != null) sp.setDoiTuong(firstRow.doiTuong);
                    if (firstRow.tinhNang != null && !firstRow.tinhNang.isEmpty()) sp.setTinhNang(firstRow.tinhNang);
                    if (firstRow.moTa != null && !firstRow.moTa.isEmpty()) sp.setMoTa(firstRow.moTa);
                    sp.setNgayCapNhat(LocalDateTime.now());
                    sanPhamRepository.save(sp);
                } else {
                    sp = new SanPham();
                    sp.setTenSanPham(firstRow.tenSp.trim());
                    sp.setMaSanPham(generateProductCode());
                    sp.setThuongHieu(th);
                    sp.setLoaiGiay(lg);
                    sp.setChatLieu(cl);
                    sp.setKieuDang(kd);
                    sp.setXuatXu(xx);
                    sp.setDoiTuong(firstRow.doiTuong);
                    sp.setTinhNang(firstRow.tinhNang);
                    sp.setMoTa(firstRow.moTa);
                    sp.setTrangThai(true);
                    sp.setNgayTao(LocalDateTime.now());
                    sp.setNgayCapNhat(LocalDateTime.now());
                    sp = sanPhamRepository.save(sp);
                    result.setSoSanPhamMoi(result.getSoSanPhamMoi() + 1);
                }

                // Lưu các biến thể
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

                for (ExcelRowData rData : rows) {
                    MauSac ms = findOrCreateMauSac(rData.mauSac);
                    KichCo kc = findOrCreateKichCo(rData.kichCo);

                    Optional<com.example.datn.entity.SanPhamChiTiet> spctOpt =
                            sanPhamChiTietRepository.findBySanPhamIdAndMauSacIdAndKichCoId(sp.getId(), ms.getId(), kc.getId());

                    if (spctOpt.isPresent()) {
                        com.example.datn.entity.SanPhamChiTiet spct = spctOpt.get();
                        int curQty = spct.getSoLuong() != null ? spct.getSoLuong() : 0;
                        int newQty = curQty + rData.soLuong;
                        spct.setSoLuong(newQty);
                        spct.setGiaBan(rData.giaBan);
                        if (newQty > 0) spct.setTrangThai(true);
                        sanPhamChiTietRepository.save(spct);
                        result.setSoBienTheCapNhat(result.getSoBienTheCapNhat() + 1);
                    } else {
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
                        spct.setSoLuong(rData.soLuong);
                        spct.setGiaBan(rData.giaBan);
                        spct.setTrangThai(true);

                        sanPhamChiTietRepository.save(spct);
                        result.setSoBienTheMoi(result.getSoBienTheMoi() + 1);
                    }

                    result.setSoDongThanhCong(result.getSoDongThanhCong() + 1);
                }
            }

            return result;
        } catch (IllegalArgumentException ex) {
            // Giữ nguyên thông báo lỗi cấu trúc/validate cụ thể cho người dùng
            throw ex;
        } catch (Exception e) {
            throw new RuntimeException("Lỗi xử lý file Excel: " + e.getMessage(), e);
        }
    }

    private static class ExcelRowData {
        int rowNumber;
        String tenSp;
        String thuongHieu;
        String loaiGiay;
        String chatLieu;
        String kieuDang;
        String xuatXu;
        String doiTuong;
        String mauSac;
        String kichCo;
        int soLuong;
        java.math.BigDecimal giaBan;
        String tinhNang;
        String moTa;
    }

    private String getCellValueAsString(org.apache.poi.ss.usermodel.Cell cell) {
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue().trim();
            case NUMERIC -> {
                double val = cell.getNumericCellValue();
                if (val == Math.floor(val)) {
                    yield String.valueOf((long) val);
                } else {
                    yield String.valueOf(val);
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case FORMULA -> {
                try {
                    yield cell.getStringCellValue().trim();
                } catch (Exception ex) {
                    try {
                        yield String.valueOf((long) cell.getNumericCellValue());
                    } catch (Exception e2) {
                        yield "";
                    }
                }
            }
            default -> "";
        };
    }

    private ThuongHieu findOrCreateThuongHieu(String name) {
        String clean = (name != null && !name.trim().isEmpty()) ? name.trim() : "Khác";
        return thuongHieuRepository.findAll().stream()
                .filter(t -> clean.equalsIgnoreCase(t.getTenThuongHieu()))
                .findFirst()
                .orElseGet(() -> {
                    ThuongHieu th = new ThuongHieu();
                    th.setTenThuongHieu(clean);
                    th.setMaThuongHieu("TH" + (System.currentTimeMillis() % 1000000));
                    th.setTrangThai(true);
                    return thuongHieuRepository.save(th);
                });
    }

    private LoaiGiay findOrCreateLoaiGiay(String name) {
        String clean = (name != null && !name.trim().isEmpty()) ? name.trim() : "Giày thể thao";
        return loaiGiayRepository.findAll().stream()
                .filter(l -> clean.equalsIgnoreCase(l.getTenLoaiGiay()))
                .findFirst()
                .orElseGet(() -> {
                    LoaiGiay lg = new LoaiGiay();
                    lg.setTenLoaiGiay(clean);
                    lg.setMaLoaiGiay("LG" + (System.currentTimeMillis() % 1000000));
                    lg.setTrangThai(true);
                    return loaiGiayRepository.save(lg);
                });
    }

    private ChatLieu findOrCreateChatLieu(String name) {
        String clean = (name != null && !name.trim().isEmpty()) ? name.trim() : "Vải dệt";
        return chatLieuRepository.findAll().stream()
                .filter(c -> clean.equalsIgnoreCase(c.getTenChatLieu()))
                .findFirst()
                .orElseGet(() -> {
                    ChatLieu cl = new ChatLieu();
                    cl.setTenChatLieu(clean);
                    cl.setMaChatLieu("CL" + (System.currentTimeMillis() % 1000000));
                    cl.setTrangThai(true);
                    return chatLieuRepository.save(cl);
                });
    }

    private KieuDang findOrCreateKieuDang(String name) {
        String clean = (name != null && !name.trim().isEmpty()) ? name.trim() : "Cổ thấp";
        return kieuDangRepository.findAll().stream()
                .filter(k -> clean.equalsIgnoreCase(k.getTenKieuDang()))
                .findFirst()
                .orElseGet(() -> {
                    KieuDang kd = new KieuDang();
                    kd.setTenKieuDang(clean);
                    kd.setMaKieuDang("KD" + (System.currentTimeMillis() % 1000000));
                    kd.setTrangThai(true);
                    return kieuDangRepository.save(kd);
                });
    }

    private XuatXu findOrCreateXuatXu(String name) {
        String clean = (name != null && !name.trim().isEmpty()) ? name.trim() : "Việt Nam";
        return xuatXuRepository.findAll().stream()
                .filter(x -> clean.equalsIgnoreCase(x.getTenXuatXu()))
                .findFirst()
                .orElseGet(() -> {
                    XuatXu xx = new XuatXu();
                    xx.setTenXuatXu(clean);
                    xx.setMaXuatXu("XX" + (System.currentTimeMillis() % 1000000));
                    xx.setTrangThai(true);
                    return xuatXuRepository.save(xx);
                });
    }

    private MauSac findOrCreateMauSac(String name) {
        String clean = (name != null && !name.trim().isEmpty()) ? name.trim() : "Trắng";
        return mauSacRepository.findAll().stream()
                .filter(m -> clean.equalsIgnoreCase(m.getTenMau()))
                .findFirst()
                .orElseGet(() -> {
                    MauSac ms = new MauSac();
                    ms.setTenMau(clean);
                    ms.setMaMau("MS" + (System.currentTimeMillis() % 1000000));
                    ms.setTrangThai(true);
                    return mauSacRepository.save(ms);
                });
    }

    private KichCo findOrCreateKichCo(String name) {
        String clean = (name != null && !name.trim().isEmpty()) ? name.trim() : "40";
        return kichCoRepository.findAll().stream()
                .filter(k -> clean.equalsIgnoreCase(k.getTenKichCo()))
                .findFirst()
                .orElseGet(() -> {
                    KichCo kc = new KichCo();
                    kc.setTenKichCo(clean);
                    kc.setMaKichCo("KC" + (System.currentTimeMillis() % 1000000));
                    kc.setTrangThai(true);
                    return kichCoRepository.save(kc);
                });
    }
}
