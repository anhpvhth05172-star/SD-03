package com.example.datn.controller;

import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaDetailResponse;
import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaRequest;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.service.PhieuGiamGiaService;
import com.example.datn.service.impls.PhieuGiamGiaServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/admin/phieu-giam-gia")
@CrossOrigin("*")
public class PhieuGiamGiaRestController {

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @Autowired
    private PhieuGiamGiaServiceImpl phieuGiamGiaServiceImpl;

    @GetMapping
    public Page<PhieuGiamGia> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "8") int size,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String loaiGiamGia,
            @RequestParam(required = false) String trangThai,
            @RequestParam(required = false) String phamVi,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay) {

        return phieuGiamGiaServiceImpl.search(keyword, loaiGiamGia, trangThai, phamVi, tuNgay, denNgay, page, size);
    }

//    @PostMapping("/add")
//    @ResponseStatus(HttpStatus.CREATED)
//    public void add(@RequestBody PhieuGiamGia phieuGiamGia) {
//        phieuGiamGiaService.addPhieuGiamGia(phieuGiamGia);
//    }

//    @GetMapping("/detail/{id}")
//    public PhieuGiamGia detail(@PathVariable Long id) {
//        return phieuGiamGiaService.detail(id);
//    }

//    @PutMapping("/update")
//    public String update(@RequestBody PhieuGiamGia phieuGiamGia) {
//        phieuGiamGiaService.update(phieuGiamGia);
//
//        return "sua thanh cong!";
//    }

//    @GetMapping("/khach-hang/all")
//    public List<KhachHangOptionResponse> danhSachKhachHang() {
//        return service.layDanhSachKhachHang();
//    }

    @PostMapping("/add")
    public ResponseEntity<Map<String, Object>> them(@RequestBody PhieuGiamGiaRequest req) {
        Long id = phieuGiamGiaServiceImpl.themPhieuGiamGia(req);
        return ResponseEntity.ok(Map.of("id", id, "message", "Tạo phiếu giảm giá thành công"));
    }

    @GetMapping("/detail/{id}")
    public PhieuGiamGiaDetailResponse chiTiet(@PathVariable Long id) {
        return phieuGiamGiaServiceImpl.chiTiet(id);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> update(
            @PathVariable Long id,
            @RequestBody PhieuGiamGiaRequest request) {

        Long updatedId = phieuGiamGiaServiceImpl.capNhatPhieuGiamGia(id, request);
        return ResponseEntity.ok(Map.of(
                "message", "Cập nhật phiếu giảm giá thành công",
                "id", updatedId
        ));
    }

    @PutMapping("/toggle-trang-thai/{id}")
    public ResponseEntity<?> toggleTrangThai(@PathVariable Long id) {
        Boolean newStatus = phieuGiamGiaServiceImpl.toggleTrangThai(id);
        return ResponseEntity.ok(Map.of(
                "message", newStatus ? "Đã bật phiếu giảm giá" : "Đã tắt phiếu giảm giá",
                "trangThai", newStatus
        ));
    }
}
