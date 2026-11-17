package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.ThongKe.DonHangThongKeDTO;
import com.example.datn.dto.ThongKe.EmailMacDinhDTO;
import com.example.datn.dto.ThongKe.GuiMailTongKetRequest;
import com.example.datn.dto.ThongKe.GuiMailTongKetResponse;
import com.example.datn.dto.ThongKe.SanPhamDaBanChiTietDTO;
import com.example.datn.dto.ThongKe.SanPhamTheoNgayDTO;
import com.example.datn.dto.ThongKe.SoSanhDTO;
import com.example.datn.dto.ThongKe.ThongKeSanPhamBangDTO;
import com.example.datn.dto.ThongKe.ThuongHieuThongKeResponse;
import com.example.datn.dto.ThongKe.TongQuanDTO;
import com.example.datn.dto.ThongKe.TrangThaiDonDTO;
import com.example.datn.service.BaoCaoTongKetService;
import com.example.datn.service.ThongKeService;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/thong-ke")
@RequiredArgsConstructor
public class ThongKeController {

    private final ThongKeService thongKeService;
    private final QuyenGuard quyenGuard;
    private final BaoCaoTongKetService baoCaoTongKetService;

    @GetMapping("/tong-quan")
    public TongQuanDTO tongQuan(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false) String thuongHieu
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.tongQuan(tu, den, thuongHieu);
    }

    @GetMapping("/trang-thai-don")
    public List<TrangThaiDonDTO> trangThaiDon(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false) String thuongHieu
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.trangThaiDon(tu, den, thuongHieu);
    }

    @GetMapping("/so-sanh")
    public SoSanhDTO soSanh(
        @RequestParam String cheDo,
        @RequestParam(required = false) Integer nam,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu1,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den1,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu2,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den2,
        @RequestParam(required = false) String thuongHieu,
        @RequestParam(required = false) String keyword
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.soSanh(cheDo, nam, tu1, den1, tu2, den2, thuongHieu, keyword);
    }

    @PostMapping("/gui-mail-tong-ket")
    public GuiMailTongKetResponse guiMailTongKet(@RequestBody GuiMailTongKetRequest req) {
        quyenGuard.canQuanLyKhachHang();
        return baoCaoTongKetService.guiMailTongKet(req);
    }

    @GetMapping("/email-mac-dinh")
    public EmailMacDinhDTO emailMacDinh() {
        quyenGuard.canQuanLyKhachHang();
        return new EmailMacDinhDTO(baoCaoTongKetService.emailMacDinh());
    }

    @GetMapping("/san-pham-theo-ngay")
    public List<SanPhamTheoNgayDTO> sanPhamTheoNgay(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false) String thuongHieu
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.sanPhamTheoNgay(tu, den, thuongHieu);
    }

    @GetMapping("/thuong-hieu")
    public ThuongHieuThongKeResponse thuongHieu(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.thuongHieuThongKe(tu, den);
    }

    @GetMapping("/san-pham-bang")
    public List<ThongKeSanPhamBangDTO> sanPhamBang(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false) String thuongHieu,
        @RequestParam(required = false) String keyword
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.sanPhamBang(tu, den, thuongHieu, keyword);
    }

    @GetMapping("/don-hang-chi-tiet")
    public List<DonHangThongKeDTO> donHangChiTiet(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false, defaultValue = "ALL") String kpiType,
        @RequestParam(required = false, defaultValue = "ALL") String loaiDon,
        @RequestParam(required = false, defaultValue = "ALL") String hinhThucTT,
        @RequestParam(required = false) String trangThai,
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String thuongHieu
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.donHangChiTiet(tu, den, kpiType, loaiDon, hinhThucTT, trangThai, keyword, thuongHieu);
    }

    @GetMapping("/san-pham-da-ban")
    public List<SanPhamDaBanChiTietDTO> sanPhamDaBan(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tu,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate den,
        @RequestParam(required = false) String thuongHieu,
        @RequestParam(required = false) String keyword
    ) {
        quyenGuard.canQuanLyKhachHang();
        return thongKeService.sanPhamDaBanChiTiet(tu, den, thuongHieu, keyword);
    }
}
