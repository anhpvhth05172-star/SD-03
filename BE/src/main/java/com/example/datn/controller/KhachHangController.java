package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.KhachHang.GuiEmailHangLoatRequest;
import com.example.datn.dto.KhachHang.GuiEmailHangLoatResponse;
import com.example.datn.dto.KhachHang.KhachHangDTO;
import com.example.datn.dto.KhachHang.KhachHangRequest;
import com.example.datn.dto.Common.PageResponse;
import com.example.datn.entity.KhachHang;
import com.example.datn.repository.KhachHangRepository;
import com.example.datn.service.KhachHangService;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/khach-hang")
@RequiredArgsConstructor
public class KhachHangController {

    private final KhachHangService khachHangService;
    private final QuyenGuard quyenGuard;

    private final KhachHangRepository khachHangRepository;

    private final com.example.datn.service.AuthService authService;

    @PutMapping("/{id}/doi-mat-khau")
    public ResponseEntity<Map<String, String>> doiMatKhau(
        @PathVariable Long id,
        @RequestBody com.example.datn.dto.Auth.DoiMatKhauRequest request
    ) {
        quyenGuard.canQuanLyKhachHang();
        authService.doiMatKhauKhachHang(id, request.getMatKhauMoi(), request.getXacNhan());
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    @GetMapping("/all")
    public List<Map<String, Object>> getList() {
        return khachHangRepository.findAll().stream()
            .map(k -> {
                Map<String, Object> m = new java.util.HashMap<>();
                m.put("id", k.getId());
                m.put("maKhachHang", k.getMaKhachHang());
                m.put("tenKhachHang", k.getTenKhachHang());
                m.put("email", k.getEmail());
                m.put("soDienThoai", k.getSoDienThoai());
                m.put("ngaySinh", k.getNgaySinh());
                return m;
            })
            .toList();
    }

    @GetMapping
    public PageResponse<KhachHangDTO> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) String hangThanhVien,
        @RequestParam(required = false) String trangThai,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return khachHangService.list(keyword, hangThanhVien, trangThai, tuNgay, denNgay, page, size);
    }

    @GetMapping("/ma-tu-dong")
    public Map<String, String> maKhachHangTuDong(@RequestParam(required = false) String ten) {
        quyenGuard.canQuanLyKhachHang();
        return Map.of("ma", khachHangService.maKhachHangTuDong(ten));
    }

    @GetMapping("/check-email")
    public Map<String, Boolean> checkEmail(
        @RequestParam String email,
        @RequestParam(required = false) Long excludeId
    ) {
        return Map.of("exists", khachHangService.checkTrungEmail(email, excludeId));
    }

    @GetMapping("/{id}")
    public KhachHangDTO get(@PathVariable Long id) {
        return khachHangService.get(id);
    }

    @PostMapping
    public ResponseEntity<KhachHangDTO> create(@RequestBody KhachHangRequest request) {
        quyenGuard.canQuanLyKhachHang();
        return ResponseEntity.status(HttpStatus.CREATED).body(khachHangService.create(request));
    }

    @PutMapping("/{id}")
    public KhachHangDTO update(@PathVariable Long id, @RequestBody KhachHangRequest request) {
        quyenGuard.canQuanLyKhachHang();
        return khachHangService.update(id, request);
    }

    @PutMapping("/{id}/trang-thai")
    public KhachHangDTO capNhatTrangThai(
        @PathVariable Long id,
        @RequestParam String trangThai
    ) {
        quyenGuard.canQuanLyKhachHang();
        return khachHangService.capNhatTrangThai(id, trangThai);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quyenGuard.canQuanLyKhachHang();
        khachHangService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/gui-email-hang-loat")
    public GuiEmailHangLoatResponse guiEmailHangLoat(
        @RequestBody GuiEmailHangLoatRequest request
    ) {
        quyenGuard.canQuanLyKhachHang();
        return khachHangService.guiEmailHangLoat(request);
    }
}
