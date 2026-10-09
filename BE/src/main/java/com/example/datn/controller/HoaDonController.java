package com.example.datn.controller;

import com.example.datn.dto.FormDataResponse;
import com.example.datn.dto.HoaDonDTO;
import com.example.datn.dto.HoaDonRequest;
import com.example.datn.dto.PageResponse;
import com.example.datn.dto.TrangThaiHoaDonRequest;
import com.example.datn.service.HoaDonService;
import java.time.LocalDate;
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
@RequestMapping("/api/hoa-don")
@RequiredArgsConstructor
public class HoaDonController {

    private final HoaDonService hoaDonService;

    @GetMapping
    public PageResponse<HoaDonDTO> list(
        @RequestParam(required = false) String ma,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
        @RequestParam(required = false) String loaiDon,
        @RequestParam(required = false) String trangThai,
        @RequestParam(defaultValue = "false") boolean daXoa,
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size
    ) {
        return hoaDonService.list(ma, tuNgay, denNgay, loaiDon, trangThai, daXoa, page, size);
    }

    @GetMapping("/form-data")
    public FormDataResponse formData() {
        return hoaDonService.formData();
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
        @RequestParam(required = false) String ma,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
        @RequestParam(required = false) String loaiDon,
        @RequestParam(required = false) String trangThai
    ) {
        byte[] file = hoaDonService.xuatExcel(ma, tuNgay, denNgay, loaiDon, trangThai);
        String filename = "Danh_sach_hoa_don_" + LocalDate.now() + ".xlsx";
        return ResponseEntity.ok()
            .header("Content-Type", "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
            .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
            .header("Content-Length", String.valueOf(file.length))
            .body(file);
    }

    @GetMapping("/{id}")
    public HoaDonDTO get(@PathVariable Long id) {
        return hoaDonService.get(id);
    }

    @PostMapping
    public ResponseEntity<HoaDonDTO> create(@RequestBody HoaDonRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(hoaDonService.create(request));
    }

    @PutMapping("/{id}")
    public HoaDonDTO update(@PathVariable Long id, @RequestBody HoaDonRequest request) {
        return hoaDonService.update(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        hoaDonService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/khoi-phuc")
    public HoaDonDTO restore(@PathVariable Long id) {
        return hoaDonService.restore(id);
    }

    @PutMapping("/{id}/trang-thai")
    public HoaDonDTO doiTrangThai(@PathVariable Long id, @RequestBody TrangThaiHoaDonRequest request) {
        return hoaDonService.doiTrangThai(id, request.getTrangThai());
    }
}
