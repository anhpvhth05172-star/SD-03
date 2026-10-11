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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public Page<PhieuGiamGia> getList(@RequestParam(name = "page", defaultValue = "0") Integer page,
                                         @RequestParam(name = "size", defaultValue = "8") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return phieuGiamGiaService.getPageList(pageable);
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

    @PutMapping("/update")
    public String update(@RequestBody PhieuGiamGia phieuGiamGia) {
        phieuGiamGiaService.update(phieuGiamGia);

        return "sua thanh cong!";
    }

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
}
