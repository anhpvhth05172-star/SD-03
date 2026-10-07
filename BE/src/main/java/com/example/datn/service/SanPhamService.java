package com.example.datn.service;

import com.example.datn.dto.SanPhamChiTietDTO;
import com.example.datn.dto.SanPhamChiTietRequest;
import com.example.datn.dto.SanPhamDTO;
import com.example.datn.dto.SanPhamRequest;
import com.example.datn.dto.ThuocTinhResponse;
import com.example.datn.entity.ChatLieu;
import com.example.datn.entity.DeGiay;
import com.example.datn.entity.KichCo;
import com.example.datn.entity.KieuDang;
import com.example.datn.entity.LoaiGiay;
import com.example.datn.entity.MauSac;
import com.example.datn.entity.SanPham;
import com.example.datn.entity.SanPhamChiTiet;
import com.example.datn.entity.ThanGiay;
import com.example.datn.entity.ThuongHieu;
import com.example.datn.entity.XuatXu;
import com.example.datn.repository.ChatLieuRepository;
import com.example.datn.repository.DeGiayRepository;
import com.example.datn.repository.KichCoRepository;
import com.example.datn.repository.KieuDangRepository;
import com.example.datn.repository.LoaiGiayRepository;
import com.example.datn.repository.MauSacRepository;
import com.example.datn.repository.SanPhamChiTietRepository;
import com.example.datn.repository.SanPhamRepository;
import com.example.datn.repository.ThanGiayRepository;
import com.example.datn.repository.ThuongHieuRepository;
import com.example.datn.repository.XuatXuRepository;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SanPhamService {

    private final SanPhamRepository sanPhamRepository;
    private final SanPhamChiTietRepository sanPhamChiTietRepository;
    private final LoaiGiayRepository loaiGiayRepository;
    private final ThuongHieuRepository thuongHieuRepository;
    private final ChatLieuRepository chatLieuRepository;
    private final XuatXuRepository xuatXuRepository;
    private final KieuDangRepository kieuDangRepository;
    private final MauSacRepository mauSacRepository;
    private final KichCoRepository kichCoRepository;
    private final ThanGiayRepository thanGiayRepository;
    private final DeGiayRepository deGiayRepository;

    @Transactional(readOnly = true)
    public List<SanPhamDTO> layDanhSach(String keyword) {
        Map<Long, Object[]> tongHop = new HashMap<>();
        for (Object[] row : sanPhamChiTietRepository.tongHopTheoSanPham()) {
            tongHop.put((Long) row[0], row);
        }
        String kw = keyword == null ? "" : keyword.trim().toLowerCase();
        return sanPhamRepository.findAll().stream()
            .filter(sp ->
                kw.isEmpty()
                    || (sp.getMaSanPham() != null && sp.getMaSanPham().toLowerCase().contains(kw))
                    || (sp.getTenSanPham() != null && sp.getTenSanPham().toLowerCase().contains(kw))
            )
            .map(sp -> {
                Object[] hop = tongHop.get(sp.getId());
                Object[] tomTat = hop == null ? null : new Object[] { hop[1], hop[2], hop[3] };
                return toDTO(sp, tomTat);
            })
            .toList();
    }

    @Transactional(readOnly = true)
    public SanPhamDTO laySanPham(Long id) {
        SanPham sp = timSanPham(id);
        List<SanPhamChiTiet> bienThe = sanPhamChiTietRepository.findBySanPhamId(id);
        return toDTO(sp, tinhTomTat(bienThe));
    }

    @Transactional
    public SanPhamDTO taoSanPham(SanPhamRequest req) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu sản phẩm không được để trống");
        }
        String ma = req.maSanPham() == null ? "" : req.maSanPham().trim();
        String ten = req.tenSanPham() == null ? "" : req.tenSanPham().trim();
        if (ma.isEmpty()) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống");
        }
        if (ten.isEmpty()) {
            throw new IllegalArgumentException("Tên sản phẩm không được để trống");
        }
        if (sanPhamRepository.existsByMaSanPham(ma)) {
            throw new IllegalArgumentException("Mã sản phẩm '" + ma + "' đã tồn tại");
        }

        SanPham sp = new SanPham();
        sp.setMaSanPham(ma);
        sp.setTenSanPham(ten);
        sp.setDoiTuong(req.doiTuong());
        sp.setTinhNang(req.tinhNang());
        sp.setMoTa(req.moTa());
        sp.setLoaiGiay(req.idLoaiGiay() == null
            ? null
            : loaiGiayRepository.findById(req.idLoaiGiay())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy loại giày #" + req.idLoaiGiay())));
        sp.setThuongHieu(req.idThuongHieu() == null
            ? null
            : thuongHieuRepository.findById(req.idThuongHieu())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thương hiệu #" + req.idThuongHieu())));
        sp.setChatLieu(req.idChatLieu() == null
            ? null
            : chatLieuRepository.findById(req.idChatLieu())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy chất liệu #" + req.idChatLieu())));
        sp.setXuatXu(req.idXuatXu() == null
            ? null
            : xuatXuRepository.findById(req.idXuatXu())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy xuất xứ #" + req.idXuatXu())));
        sp.setKieuDang(req.idKieuDang() == null
            ? null
            : kieuDangRepository.findById(req.idKieuDang())
                .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kiểu dáng #" + req.idKieuDang())));

        if (sp.getLoaiGiay() == null) {
            throw new IllegalArgumentException("Loại giày là bắt buộc");
        }
        if (sp.getThuongHieu() == null) {
            throw new IllegalArgumentException("Thương hiệu là bắt buộc");
        }
        if (sp.getChatLieu() == null) {
            throw new IllegalArgumentException("Chất liệu là bắt buộc");
        }
        if (sp.getXuatXu() == null) {
            throw new IllegalArgumentException("Xuất xứ là bắt buộc");
        }
        if (sp.getKieuDang() == null) {
            throw new IllegalArgumentException("Kiểu dáng là bắt buộc");
        }

        sp.setTrangThai(req.trangThai() == null || req.trangThai());
        sp = sanPhamRepository.save(sp);

        List<SanPhamChiTietRequest> danhSachBienThe = req.bienThe() == null ? List.of() : req.bienThe();
        Set<String> maTrongRequest = new HashSet<>();
        for (SanPhamChiTietRequest ct : danhSachBienThe) {
            sanPhamChiTietRepository.save(taoEntityBienThe(sp, ct, maTrongRequest));
        }

        List<SanPhamChiTiet> bienTheDaTao = sanPhamChiTietRepository.findBySanPhamId(sp.getId());
        return toDTO(sp, tinhTomTat(bienTheDaTao));
    }

    @Transactional(readOnly = true)
    public List<SanPhamChiTietDTO> layBienThe(Long idSanPham) {
        timSanPham(idSanPham);
        return sanPhamChiTietRepository.findBySanPhamId(idSanPham).stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public SanPhamChiTietDTO taoBienThe(Long idSanPham, SanPhamChiTietRequest req) {
        SanPham sp = timSanPham(idSanPham);
        SanPhamChiTiet ct = taoEntityBienThe(sp, req, new HashSet<>());
        return toDTO(sanPhamChiTietRepository.save(ct));
    }

    @Transactional(readOnly = true)
    public ThuocTinhResponse layThuocTinh() {
        List<ThuocTinhResponse.Item> loaiGiay = loaiGiayRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), x.getMaLoaiGiay(), x.getTenLoaiGiay()))
            .toList();
        List<ThuocTinhResponse.Item> thuongHieu = thuongHieuRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), x.getMaThuongHieu(), x.getTenThuongHieu()))
            .toList();
        List<ThuocTinhResponse.Item> chatLieu = chatLieuRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), x.getMaChatLieu(), x.getTenChatLieu()))
            .toList();
        List<ThuocTinhResponse.Item> xuatXu = xuatXuRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), x.getMaXuatXu(), x.getTenXuatXu()))
            .toList();
        List<ThuocTinhResponse.Item> kieuDang = kieuDangRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), x.getMaKieuDang(), x.getTenKieuDang()))
            .toList();
        List<ThuocTinhResponse.Item> mauSac = mauSacRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), x.getMaMau(), x.getTenMau()))
            .toList();
        List<ThuocTinhResponse.Item> kichCo = kichCoRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), x.getMaKichCo(), x.getTenKichCo()))
            .toList();
        List<ThuocTinhResponse.Item> thanGiay = thanGiayRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), null, x.getTenThanGiay()))
            .toList();
        List<ThuocTinhResponse.Item> deGiay = deGiayRepository.findAll().stream()
            .filter(x -> Boolean.TRUE.equals(x.getTrangThai()))
            .map(x -> new ThuocTinhResponse.Item(x.getId(), null, x.getTenDeGiay()))
            .toList();
        return new ThuocTinhResponse(
            loaiGiay, thuongHieu, chatLieu, xuatXu, kieuDang, mauSac, kichCo, thanGiay, deGiay
        );
    }

    private SanPhamChiTiet taoEntityBienThe(
        SanPham sp,
        SanPhamChiTietRequest req,
        Set<String> maTrongRequest
    ) {
        if (req == null) {
            throw new IllegalArgumentException("Dữ liệu biến thể không được để trống");
        }
        String ma = req.maChiTietSanPham() == null ? "" : req.maChiTietSanPham().trim();
        if (ma.isEmpty()) {
            throw new IllegalArgumentException("Mã biến thể không được để trống");
        }
        if (sanPhamChiTietRepository.existsByMaChiTietSanPham(ma) || !maTrongRequest.add(ma)) {
            throw new IllegalArgumentException("Mã biến thể '" + ma + "' đã tồn tại");
        }
        if (req.giaBan() == null || req.giaBan().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Giá bán của biến thể '" + ma + "' phải lớn hơn 0");
        }
        if (req.soLuong() != null && req.soLuong() < 0) {
            throw new IllegalArgumentException("Số lượng của biến thể '" + ma + "' không được âm");
        }
        if (req.idMauSac() == null) {
            throw new IllegalArgumentException("Màu sắc của biến thể '" + ma + "' là bắt buộc");
        }
        if (req.idKichCo() == null) {
            throw new IllegalArgumentException("Kích cỡ của biến thể '" + ma + "' là bắt buộc");
        }
        if (req.idThanGiay() == null) {
            throw new IllegalArgumentException("Thân giày của biến thể '" + ma + "' là bắt buộc");
        }
        if (req.idDeGiay() == null) {
            throw new IllegalArgumentException("Đế giày của biến thể '" + ma + "' là bắt buộc");
        }

        MauSac mauSac = mauSacRepository.findById(req.idMauSac())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy màu sắc #" + req.idMauSac()));
        KichCo kichCo = kichCoRepository.findById(req.idKichCo())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy kích cỡ #" + req.idKichCo()));
        ThanGiay thanGiay = thanGiayRepository.findById(req.idThanGiay())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy thân giày #" + req.idThanGiay()));
        DeGiay deGiay = deGiayRepository.findById(req.idDeGiay())
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy đế giày #" + req.idDeGiay()));

        SanPhamChiTiet ct = new SanPhamChiTiet();
        ct.setSanPham(sp);
        ct.setMaChiTietSanPham(ma);
        ct.setMauSac(mauSac);
        ct.setKichCo(kichCo);
        ct.setThanGiay(thanGiay);
        ct.setDeGiay(deGiay);
        ct.setGiaBan(req.giaBan());
        ct.setSoLuong(req.soLuong() == null ? 0 : req.soLuong());
        ct.setTrangThai(req.trangThai() == null || req.trangThai());
        ct.setTrongLuong(req.trongLuong());
        ct.setChieuCaoDe(req.chieuCaoDe());
        ct.setChieuCaoGot(req.chieuCaoGot());
        ct.setDoChenhGotMui(req.doChenhGotMui());
        ct.setFormGiay(req.formGiay());
        return ct;
    }

    private SanPham timSanPham(Long id) {
        if (id == null) {
            throw new IllegalArgumentException("Mã sản phẩm không được để trống");
        }
        return sanPhamRepository.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Không tìm thấy sản phẩm #" + id));
    }

    private Object[] tinhTomTat(List<SanPhamChiTiet> bienThe) {
        long soLuongBienThe = bienThe.size();
        long tongTonKho = 0L;
        BigDecimal giaNhoNhat = null;
        for (SanPhamChiTiet ct : bienThe) {
            tongTonKho += ct.getSoLuong() == null ? 0 : ct.getSoLuong();
            if (ct.getGiaBan() != null && (giaNhoNhat == null || ct.getGiaBan().compareTo(giaNhoNhat) < 0)) {
                giaNhoNhat = ct.getGiaBan();
            }
        }
        return new Object[] { soLuongBienThe, tongTonKho, giaNhoNhat };
    }

    private SanPhamDTO toDTO(SanPham sp, Object[] tomTat) {
        long soLuongBienThe = tomTat == null ? 0L : (Long) tomTat[0];
        long tongTonKho = tomTat == null ? 0L : (Long) tomTat[1];
        BigDecimal giaNhoNhat = tomTat == null ? null : (BigDecimal) tomTat[2];
        return toDTO(sp, soLuongBienThe, tongTonKho, giaNhoNhat);
    }

    private SanPhamDTO toDTO(SanPham sp, long soLuongBienThe, long tongTonKho, BigDecimal giaNhoNhat) {
        return new SanPhamDTO(
            sp.getId(),
            sp.getMaSanPham(),
            sp.getTenSanPham(),
            sp.getDoiTuong(),
            sp.getMoTa(),
            sp.getLoaiGiay() == null ? null : sp.getLoaiGiay().getTenLoaiGiay(),
            sp.getThuongHieu() == null ? null : sp.getThuongHieu().getTenThuongHieu(),
            sp.getChatLieu() == null ? null : sp.getChatLieu().getTenChatLieu(),
            sp.getXuatXu() == null ? null : sp.getXuatXu().getTenXuatXu(),
            sp.getKieuDang() == null ? null : sp.getKieuDang().getTenKieuDang(),
            (int) soLuongBienThe,
            tongTonKho,
            giaNhoNhat,
            sp.getTrangThai(),
            sp.getNgayTao()
        );
    }

    private SanPhamChiTietDTO toDTO(SanPhamChiTiet ct) {
        return new SanPhamChiTietDTO(
            ct.getId(),
            ct.getSanPham() == null ? null : ct.getSanPham().getId(),
            ct.getMaChiTietSanPham(),
            ct.getMauSac() == null ? null : ct.getMauSac().getTenMau(),
            ct.getKichCo() == null ? null : ct.getKichCo().getTenKichCo(),
            ct.getThanGiay() == null ? null : ct.getThanGiay().getTenThanGiay(),
            ct.getDeGiay() == null ? null : ct.getDeGiay().getTenDeGiay(),
            ct.getSoLuong(),
            ct.getGiaBan(),
            ct.getTrangThai(),
            ct.getTrongLuong(),
            ct.getChieuCaoDe(),
            ct.getChieuCaoGot(),
            ct.getDoChenhGotMui(),
            ct.getFormGiay(),
            ct.getNgayTao()
        );
    }
}
