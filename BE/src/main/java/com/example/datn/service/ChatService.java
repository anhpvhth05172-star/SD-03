package com.example.datn.service;

import com.example.datn.dto.Chat.HoiThoaiDTO;
import com.example.datn.dto.Chat.TinNhanDTO;
import com.example.datn.entity.HoiThoai;
import com.example.datn.entity.KhachHang;
import com.example.datn.entity.TinNhan;
import com.example.datn.exception.KhongCoQuyenException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.HoiThoaiRepository;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.repository.TinNhanRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatService {

    private final HoiThoaiRepository hoiThoaiRepository;
    private final TinNhanRepository tinNhanRepository;
    private final KhachHangRepository khachHangRepository;
    private final com.example.datn.repository.NhanVienRepository nhanVienRepository;

    /**
     * Lay hoac tao hoi thoai nhan vien cho khach.
     * - Khach dang nhap: dung idKhachHang, bo qua maAnDanh.
     * - Khach chua dang nhap: bat buoc maAnDanh (UUID an danh tu FE), hash de luu.
     * Khong tin UUID FE gui len thieu xac minh: chi dung de tim/tao theo hash.
     */
    @Transactional
    public HoiThoaiDTO layHoiThoaiNhanVien(Long idKhachHang, String maAnDanh) {
        if (idKhachHang != null) {
            return hoiThoaiRepository
                .findFirstByLoaiAndKhachHangIdAndTrangThaiOrderByNgayCapNhatDesc(
                    HoiThoai.LOAI_NHAN_VIEN,
                    idKhachHang,
                    HoiThoai.TRANG_THAI_DANG_MO
                )
                .map(this::toKhachDTO)
                .orElseGet(() -> {
                    KhachHang kh = khachHangRepository.findById(idKhachHang).orElse(null);
                    return toKhachDTO(
                        taoHoiThoaiNhanVien(idKhachHang, null, kh == null ? null : kh.getTenTaiKhoan())
                    );
                });
        }
        String hash = hashMaAnDanh(maAnDanh);
        return hoiThoaiRepository
            .findFirstByLoaiAndMaAnDanhAndTrangThaiOrderByNgayCapNhatDesc(
                HoiThoai.LOAI_NHAN_VIEN,
                hash,
                HoiThoai.TRANG_THAI_DANG_MO
            )
            .map(this::toKhachDTO)
            .orElseGet(() -> toKhachDTO(taoHoiThoaiNhanVien(null, hash, "Khách ẩn danh")));
    }

    /**
     * Lien ket hoi thoai an danh voi tai khoan khach sau khi BE xac minh danh tinh.
     */
    @Transactional
    public HoiThoaiDTO lienKetVaDangNhap(Long idKhachHang, String maAnDanh) {
        KhachHang kh = khachHangRepository
            .findById(idKhachHang)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay khach hang"));
        String hash = hashMaAnDanh(maAnDanh);
        HoiThoai coTrong = hoiThoaiRepository
            .findFirstByLoaiAndMaAnDanhAndTrangThaiOrderByNgayCapNhatDesc(
                HoiThoai.LOAI_NHAN_VIEN,
                hash,
                HoiThoai.TRANG_THAI_DANG_MO
            )
            .orElse(null);
        HoiThoai cuaKh = hoiThoaiRepository
            .findFirstByLoaiAndKhachHangIdAndTrangThaiOrderByNgayCapNhatDesc(
                HoiThoai.LOAI_NHAN_VIEN,
                idKhachHang,
                HoiThoai.TRANG_THAI_DANG_MO
            )
            .orElse(null);

        if (coTrong != null && cuaKh != null && !coTrong.getId().equals(cuaKh.getId())) {
            for (TinNhan t : tinNhanRepository.timTheoHoiThoai(coTrong.getId())) {
                t.setHoiThoai(cuaKh);
                tinNhanRepository.save(t);
            }
            coTrong.setTrangThai(HoiThoai.TRANG_THAI_DONG);
            coTrong.setMaAnDanh(null);
            coTrong.setNgayCapNhat(LocalDateTime.now());
            hoiThoaiRepository.save(coTrong);
            cuaKh.setNgayCapNhat(LocalDateTime.now());
            hoiThoaiRepository.save(cuaKh);
            return toKhachDTO(cuaKh);
        }
        if (coTrong != null) {
            coTrong.setKhachHang(kh);
            coTrong.setTenKhach(kh.getTenKhachHang());
            coTrong.setMaAnDanh(null);
            coTrong.setNgayCapNhat(LocalDateTime.now());
            return toKhachDTO(hoiThoaiRepository.save(coTrong));
        }
        if (cuaKh != null) {
            return toKhachDTO(cuaKh);
        }
        return toKhachDTO(taoHoiThoaiNhanVien(idKhachHang, null, kh.getTenKhachHang()));
    }

    /**
     * Khach gui tin nhan: can idHoiThoai + (idKhachHang hoac maAnDanh khop hoi thoai).
     */
    @Transactional
    public TinNhanDTO guiTinKhach(Long idHoiThoai, Long idKhachHang, String maAnDanh, String noiDung) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_NHAN_VIEN)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai"));
        xacMinhSoHuu(hoiThoai, idKhachHang, maAnDanh);
        if (!HoiThoai.TRANG_THAI_DANG_MO.equals(hoiThoai.getTrangThai())) {
            throw new IllegalStateException("Hoi thoai da dong");
        }
        return guiTin(hoiThoai, TinNhan.GUI_KHACH, idKhachHang, noiDung);
    }

    /**
     * Nhan vien gui tin: hop thu chung - moi nhan vien/quan tri vien deu tra loi duoc
     * moi hoi thoai (khong con gioi han "phu trach rieng").
     */
    @Transactional
    public TinNhanDTO guiTinNhanVien(Long idHoiThoai, Long idNhanVien, String noiDung) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_NHAN_VIEN)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai"));
        if (hoiThoai.getNhanVienPhuTrach() == null) {
            var nv = new com.example.datn.entity.NhanVien();
            nv.setId(idNhanVien);
            hoiThoai.setNhanVienPhuTrach(nv);
        }
        if (!HoiThoai.TRANG_THAI_DANG_MO.equals(hoiThoai.getTrangThai())) {
            throw new IllegalStateException("Hoi thoai da dong");
        }
        return guiTin(hoiThoai, TinNhan.GUI_NHAN_VIEN, idNhanVien, noiDung);
    }

    /** Khach poll tin moi. */
    @Transactional(readOnly = true)
    public List<TinNhanDTO> layTinKhach(Long idHoiThoai, Long idKhachHang, String maAnDanh, Long tuId) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_NHAN_VIEN)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai"));
        xacMinhSoHuu(hoiThoai, idKhachHang, maAnDanh);
        return tinNhanRepository.timMoiHon(idHoiThoai, tuId == null ? 0L : tuId)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    /** Nhan vien poll tin moi (hop thu chung). */
    @Transactional(readOnly = true)
    public List<TinNhanDTO> layTinNhanVien(Long idHoiThoai, Long idNhanVien, Long tuId) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_NHAN_VIEN)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai"));
        return tinNhanRepository.timMoiHon(idHoiThoai, tuId == null ? 0L : tuId)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    @Transactional
    public void danhDaDocTinKhach(Long idHoiThoai, Long idKhachHang, String maAnDanh) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_NHAN_VIEN)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai"));
        xacMinhSoHuu(hoiThoai, idKhachHang, maAnDanh);
        tinNhanRepository.danhDaDoc(idHoiThoai, TinNhan.GUI_NHAN_VIEN);
    }

    @Transactional
    public void danhDaDocTinNhanVien(Long idHoiThoai, Long idNhanVien) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_NHAN_VIEN)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai"));
        tinNhanRepository.danhDaDoc(idHoiThoai, TinNhan.GUI_KHACH);
    }

    @Transactional(readOnly = true)
    public List<HoiThoaiDTO> dsHoiThoaiNhanVien(Long idNhanVien) {
        return hoiThoaiRepository.timHoiThoaiChoNhanVien(HoiThoai.LOAI_NHAN_VIEN)
            .stream()
            .map(this::toNVDTO)
            .toList();
    }

    @Transactional
    public HoiThoaiDTO dongHoiThoai(Long idHoiThoai, Long idNhanVien) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_NHAN_VIEN)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai"));
        hoiThoai.setTrangThai(HoiThoai.TRANG_THAI_DONG);
        hoiThoai.setNgayCapNhat(LocalDateTime.now());
        return toNVDTO(hoiThoaiRepository.save(hoiThoai));
    }

    // ---- AI chat ----

    @Transactional
    public HoiThoaiDTO layHoiThoaiAI(Long idKhachHang, String maAnDanh) {
        if (idKhachHang != null) {
            return hoiThoaiRepository
                .findFirstByLoaiAndKhachHangIdAndTrangThaiOrderByNgayCapNhatDesc(
                    HoiThoai.LOAI_AI, idKhachHang, HoiThoai.TRANG_THAI_DANG_MO
                )
                .map(this::toKhachDTO)
                .orElseGet(() -> {
                    HoiThoai h = new HoiThoai();
                    h.setLoai(HoiThoai.LOAI_AI);
                    h.setTrangThai(HoiThoai.TRANG_THAI_DANG_MO);
                    var kh = new KhachHang();
                    kh.setId(idKhachHang);
                    h.setKhachHang(kh);
                    h.setNgayCapNhat(LocalDateTime.now());
                    return toKhachDTO(hoiThoaiRepository.save(h));
                });
        }
        String hash = hashMaAnDanh(maAnDanh);
        return hoiThoaiRepository
            .findFirstByLoaiAndMaAnDanhAndTrangThaiOrderByNgayCapNhatDesc(
                HoiThoai.LOAI_AI, hash, HoiThoai.TRANG_THAI_DANG_MO
            )
            .map(this::toKhachDTO)
            .orElseGet(() -> {
                HoiThoai h = new HoiThoai();
                h.setLoai(HoiThoai.LOAI_AI);
                h.setTrangThai(HoiThoai.TRANG_THAI_DANG_MO);
                h.setMaAnDanh(hash);
                h.setNgayCapNhat(LocalDateTime.now());
                return toKhachDTO(hoiThoaiRepository.save(h));
            });
    }

    @Transactional
    public TinNhanDTO guiTinAIvaLuu(
        Long idHoiThoai, Long idKhachHang, String maAnDanh, String noiDungKhach, String noiDungAI
    ) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_AI)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai AI"));
        if (idKhachHang == null) {
            String hash = hashMaAnDanh(maAnDanh);
            if (hoiThoai.getMaAnDanh() == null || !hoiThoai.getMaAnDanh().equals(hash)) {
                throw new KhongCoQuyenException("Khong co quyen gui tin vao hoi thoai nay");
            }
        } else if (hoiThoai.getKhachHang() == null || !hoiThoai.getKhachHang().getId().equals(idKhachHang)) {
            throw new KhongCoQuyenException("Khong co quyen gui tin vao hoi thoai nay");
        }
        TinNhanDTO kh = guiTin(hoiThoai, TinNhan.GUI_KHACH, idKhachHang, noiDungKhach);
        guiTin(hoiThoai, TinNhan.GUI_AI, null, noiDungAI);
        return kh;
    }

    @Transactional(readOnly = true)
    public List<TinNhanDTO> layTinAI(Long idHoiThoai, Long idKhachHang, String maAnDanh, Long tuId) {
        HoiThoai hoiThoai = hoiThoaiRepository
            .findByIdAndLoai(idHoiThoai, HoiThoai.LOAI_AI)
            .orElseThrow(() -> new ResourceNotFoundException("Khong tim thay hoi thoai AI"));
        if (idKhachHang == null) {
            String hash = hashMaAnDanh(maAnDanh);
            if (hoiThoai.getMaAnDanh() == null || !hoiThoai.getMaAnDanh().equals(hash)) {
                throw new KhongCoQuyenException("Khong co quyen xem hoi thoai nay");
            }
        } else if (hoiThoai.getKhachHang() == null || !hoiThoai.getKhachHang().getId().equals(idKhachHang)) {
            throw new KhongCoQuyenException("Khong co quyen xem hoi thoai nay");
        }
        return tinNhanRepository.timMoiHon(idHoiThoai, tuId == null ? 0L : tuId)
            .stream()
            .map(this::toDTO)
            .toList();
    }

    // ---- helpers ----

    private HoiThoai taoHoiThoaiNhanVien(Long idKhachHang, String maAnDanhHash, String tenKhach) {
        HoiThoai h = new HoiThoai();
        h.setLoai(HoiThoai.LOAI_NHAN_VIEN);
        h.setTrangThai(HoiThoai.TRANG_THAI_DANG_MO);
        if (idKhachHang != null) {
            var kh = new KhachHang();
            kh.setId(idKhachHang);
            h.setKhachHang(kh);
            h.setTenKhach(tenKhach);
        } else {
            h.setMaAnDanh(maAnDanhHash);
            h.setTenKhach(tenKhach);
        }
        h.setNgayCapNhat(LocalDateTime.now());
        return hoiThoaiRepository.save(h);
    }

    private TinNhanDTO guiTin(HoiThoai hoiThoai, String loaiGui, Long idNguoiGui, String noiDung) {
        if (noiDung == null || noiDung.isBlank()) {
            throw new IllegalArgumentException("Noi dung tin nhan khong duoc de trong");
        }
        TinNhan t = new TinNhan();
        t.setHoiThoai(hoiThoai);
        t.setLoaiNguoiGui(loaiGui);
        t.setIdNguoiGui(idNguoiGui);
        t.setNoiDung(noiDung.trim());
        t.setDaDoc(false);
        TinNhan saved = tinNhanRepository.save(t);
        if (TinNhan.GUI_KHACH.equals(loaiGui)) {
            hoiThoai.setTongTinKhach(hoiThoai.getTongTinKhach() + 1);
        } else {
            hoiThoai.setTongTinNhanVien(hoiThoai.getTongTinNhanVien() + 1);
        }
        hoiThoai.setNgayCapNhat(LocalDateTime.now());
        hoiThoaiRepository.save(hoiThoai);
        return toDTO(saved);
    }

    private void xacMinhSoHuu(HoiThoai hoiThoai, Long idKhachHang, String maAnDanh) {
        if (idKhachHang != null) {
            if (hoiThoai.getKhachHang() == null || !hoiThoai.getKhachHang().getId().equals(idKhachHang)) {
                throw new KhongCoQuyenException("Ban khong co quyen truy cap hoi thoai nay");
            }
            return;
        }
        String hash = hashMaAnDanh(maAnDanh);
        if (hoiThoai.getMaAnDanh() == null || !hoiThoai.getMaAnDanh().equals(hash)) {
            throw new KhongCoQuyenException("Ban khong co quyen truy cap hoi thoai nay");
        }
    }

    /** Hash UUID an danh truoc khi luu (khong luu plaintext). */
    public static String hashMaAnDanh(String maAnDanh) {
        if (maAnDanh == null || maAnDanh.isBlank()) {
            throw new IllegalArgumentException("Thieu ma an danh hoi thoai");
        }
        String giaTri = maAnDanh.trim().toLowerCase();
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(giaTri.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public static String taoMaAnDanhMoi() {
        return UUID.randomUUID().toString();
    }

    static HoiThoaiDTO toDTO(HoiThoai h, long tinChuaDoc) {
        return new HoiThoaiDTO(
            h.getId(),
            h.getLoai(),
            h.getTrangThai(),
            h.getKhachHang() != null ? h.getKhachHang().getId() : null,
            h.getNhanVienPhuTrach() != null ? h.getNhanVienPhuTrach().getId() : null,
            h.getTenKhach(),
            h.getTongTinKhach(),
            h.getTongTinNhanVien(),
            tinChuaDoc,
            h.getNgayTao(),
            h.getNgayCapNhat(),
            null
        );
    }

    private long demTinChuaDoc(HoiThoai h, boolean dangXemNhanVien) {
        if (h.getId() == null) {
            return 0;
        }
        if (dangXemNhanVien) {
            return tinNhanRepository.demChuaDoc(h.getId(), TinNhan.GUI_KHACH);
        }
        return tinNhanRepository.demChuaDoc(h.getId(), TinNhan.GUI_NHAN_VIEN)
            + tinNhanRepository.demChuaDoc(h.getId(), TinNhan.GUI_AI);
    }

    private HoiThoaiDTO toNVDTO(HoiThoai h) {
        return toDTO(h, demTinChuaDoc(h, true));
    }

    private HoiThoaiDTO toKhachDTO(HoiThoai h) {
        return toDTO(h, demTinChuaDoc(h, false));
    }

    TinNhanDTO toDTO(TinNhan t) {
        return new TinNhanDTO(
            t.getId(),
            t.getHoiThoai() != null ? t.getHoiThoai().getId() : null,
            t.getLoaiNguoiGui(),
            t.getIdNguoiGui(),
            t.getNoiDung(),
            t.isDaDoc(),
            t.getNgayTao(),
            layTenNguoiGui(t)
        );
    }

    private String layTenNguoiGui(TinNhan t) {
        if (TinNhan.GUI_NHAN_VIEN.equals(t.getLoaiNguoiGui()) && t.getIdNguoiGui() != null) {
            return nhanVienRepository
                .findById(t.getIdNguoiGui())
                .map(com.example.datn.entity.NhanVien::getTenTaiKhoan)
                .orElse(null);
        }
        if (TinNhan.GUI_KHACH.equals(t.getLoaiNguoiGui())) {
            return t.getHoiThoai() != null ? t.getHoiThoai().getTenKhach() : null;
        }
        if (TinNhan.GUI_AI.equals(t.getLoaiNguoiGui())) {
            return "AI";
        }
        return null;
    }
}
