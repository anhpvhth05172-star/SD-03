package com.example.datn.controller;

import com.example.datn.config.QuyenGuard;
import com.example.datn.dto.NhanVien.NhanVienDTO;
import com.example.datn.dto.NhanVien.NhanVienExcelImportResult;
import com.example.datn.dto.NhanVien.NhanVienFormDataResponse;
import com.example.datn.dto.NhanVien.NhanVienRequest;
import com.example.datn.dto.Common.PageResponse;
import com.example.datn.service.NhanVienService;
import java.time.LocalDate;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/nhan-vien")
@RequiredArgsConstructor
public class NhanVienController {

    private final NhanVienService nhanVienService;
    private final QuyenGuard quyenGuard;
    private final com.example.datn.service.AuthService authService;

    @GetMapping
    public PageResponse<NhanVienDTO> list(
        @RequestParam(required = false) String keyword,
        @RequestParam(required = false) Long idVaiTro,
        @RequestParam(required = false) String trangThai,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        quyenGuard.canQuanLyNhanVien();
        return nhanVienService.list(keyword, idVaiTro, trangThai, tuNgay, denNgay, page, size);
    }

    @GetMapping("/form-data")
    public NhanVienFormDataResponse formData() {
        quyenGuard.canQuanLyNhanVien();
        return nhanVienService.formData();
    }

    @GetMapping("/ma-tu-dong")
    public Map<String, String> maNhanVienTuDong(@RequestParam(required = false) String ten) {
        quyenGuard.canQuanLyNhanVien();
        return Map.of("ma", nhanVienService.maNhanVienTuDong(ten));
    }

    @GetMapping("/excel/template")
    public ResponseEntity<byte[]> downloadExcelTemplate() {
        quyenGuard.canQuanLyNhanVien();
        byte[] bytes = nhanVienService.downloadExcelTemplate();
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION,
                "attachment; filename=\"Mau_Import_Nhan_Vien.xlsx\"")
            .contentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
            .body(bytes);
    }

    @PostMapping(value = "/excel/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NhanVienExcelImportResult> importExcel(
        @RequestParam("file") MultipartFile file
    ) {
        quyenGuard.canQuanLyNhanVien();
        return ResponseEntity.ok(nhanVienService.importFromExcel(file));
    }

    @GetMapping("/check-email")
    public Map<String, Boolean> checkEmail(
        @RequestParam String email,
        @RequestParam(required = false) Long excludeId
    ) {
        return Map.of("exists", nhanVienService.checkTrungEmail(email, excludeId));
    }

    @GetMapping("/{id}")
    public NhanVienDTO get(@PathVariable Long id) {
        quyenGuard.canQuanLyNhanVien();
        return nhanVienService.get(id);
    }

    @PostMapping
    public ResponseEntity<NhanVienDTO> create(@RequestBody NhanVienRequest request) {
        quyenGuard.canQuanLyNhanVien();
        return ResponseEntity.status(HttpStatus.CREATED).body(nhanVienService.create(request));
    }

    @PutMapping("/{id}")
    public NhanVienDTO update(@PathVariable Long id, @RequestBody NhanVienRequest request) {
        quyenGuard.canQuanLyNhanVien();
        return nhanVienService.update(id, request);
    }

    @PutMapping("/{id}/trang-thai")
    public NhanVienDTO capNhatTrangThai(
        @PathVariable Long id,
        @RequestParam String trangThai
    ) {
        quyenGuard.canQuanLyNhanVien();
        return nhanVienService.capNhatTrangThai(id, trangThai);
    }

    @PutMapping("/{id}/doi-mat-khau")
    public ResponseEntity<Map<String, String>> doiMatKhau(
        @PathVariable Long id,
        @RequestBody com.example.datn.dto.Auth.DoiMatKhauRequest request
    ) {
        quyenGuard.canQuanLyNhanVien();
        authService.doiMatKhauNhanVien(id, request.getMatKhauMoi(), request.getXacNhan());
        return ResponseEntity.ok(Map.of("message", "Đổi mật khẩu thành công"));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        quyenGuard.canQuanLyNhanVien();
        nhanVienService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
