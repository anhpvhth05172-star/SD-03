package com.example.datn.service.impl;

import com.example.datn.dto.SanPhamRequest;
import com.example.datn.dto.SanPhamResponse;
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
import com.example.datn.repository.ThuongHieuRepository;
import com.example.datn.repository.XuatXuRepository;
import com.example.datn.service.SanPhamService;
import java.time.LocalDateTime;
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
    private final XuatXuRepository xuatXuRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final KieuDangRepository kieuDangRepository;
    private final LoaiGiayRepository loaiGiayRepository;

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
        if (!sanPhamRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy sản phẩm với ID: " + id);
        }
        sanPhamRepository.deleteById(id);
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
                .trangThai(sp.getTrangThai())
                .ngayTao(sp.getNgayTao())
                .ngayCapNhat(sp.getNgayCapNhat())
                .build();
    }
}
