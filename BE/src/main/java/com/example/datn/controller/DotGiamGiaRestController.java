package com.example.datn.controller;

import com.example.datn.dto.DotGiamGia.*;
import com.example.datn.entity.DotGiamGia;
import com.example.datn.service.DotGiamGiaService;
import com.example.datn.service.impls.DotGiamGiaChiTietService;
import com.example.datn.service.impls.DotGiamGiaServiceFinal;
import com.example.datn.service.impls.DotGiamGiaServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin/dot-giam-gia")
public class DotGiamGiaRestController {

    @Autowired
    private DotGiamGiaServiceImpl dotGiamGiaService;

    @Autowired
    private DotGiamGiaServiceFinal serviceFinal;

    @GetMapping
    public Page<DotGiamGia> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String ten,
            @RequestParam(required = false) Boolean trangThai,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {

        String keyword = (ten == null || ten.isBlank()) ? null : ten.trim();
        LocalDateTime from = tuNgay == null ? null : tuNgay.atStartOfDay();
        LocalDateTime to = denNgay == null ? null : denNgay.plusDays(1).atStartOfDay();

        return dotGiamGiaService.searching(keyword, trangThai, from, to, PageRequest.of(page, size));
    }

//    @PostMapping("/add")
//    @ResponseStatus(HttpStatus.CREATED)
//    public void add(@RequestBody DotGiamGia dotGiamGia) {
//        dotGiamGiaService.addDotGiamGia(dotGiamGia);
//    }

//    @GetMapping("/detail/{id}")
//    public DotGiamGia detail(@PathVariable Long id) {
//        return dotGiamGiaService.detailDotGiamGia(id);
//    }

    @PutMapping("/{id}/trang-thai")
    public String doiTrangThaiTheoPath(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> body) {
        if (body == null || body.get("trangThai") == null) {
            throw new IllegalArgumentException("Vui lòng chọn trạng thái");
        }
        serviceFinal.doiTrangThai(id, body.get("trangThai"));
        return "ok";
    }

    @GetMapping("/san-pham-chi-tiet/for-discount")
    public List<SanPhamChiTietOptionResponse> danhSachSanPham() {
        return serviceFinal.layDanhSachSanPham();
    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> them(@RequestBody DotGiamGiaRequest req) {
        Long id = serviceFinal.them(req);
        return ResponseEntity.ok(Map.of("id", id, "message", "Tạo đợt giảm giá thành công"));
    }

    @GetMapping("/detail/{id}")
    public DotGiamGiaDetailResponse chiTiet(@PathVariable Long id) {
        return serviceFinal.chiTiet(id);
    }
    @PutMapping("/change-status/{id}")
    public ResponseEntity<DotGiamGiaTrangThaiResponse> doiTrangThai(
            @PathVariable Long id,
            @RequestBody DotGiamGiaTrangThaiRequest request) {
        Boolean ketQua = serviceFinal.doiTrangThai(id, request.getTrangThai());
        return ResponseEntity.ok(new DotGiamGiaTrangThaiResponse(id, ketQua));
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<Long> capNhat(@PathVariable Long id,
                                        @RequestBody DotGiamGiaRequest request) {
        return ResponseEntity.ok(serviceFinal.capNhat(id, request));
    }
}
