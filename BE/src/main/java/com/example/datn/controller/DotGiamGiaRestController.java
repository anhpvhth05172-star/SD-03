package com.example.datn.controller;

import com.example.datn.entity.DotGiamGia;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.service.DotGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/admin/dot-giam-gia")
@CrossOrigin("*")
public class DotGiamGiaRestController {

    @Autowired
    private DotGiamGiaService dotGiamGiaService;

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

    @PostMapping("/add")
    @ResponseStatus(HttpStatus.CREATED)
    public void add(@RequestBody DotGiamGia dotGiamGia) {
        dotGiamGiaService.addDotGiamGia(dotGiamGia);
    }

    @GetMapping("/detail/{id}")
    public DotGiamGia detail(@PathVariable Long id) {
        return dotGiamGiaService.detailDotGiamGia(id);
    }

    @PutMapping("/update")
    public String updateDGG(@RequestBody DotGiamGia dotGiamGia) {
        dotGiamGiaService.updateDotGiamGia(dotGiamGia);

        return "Sua thanh cong";
    }

    @PutMapping("/{id}/trang-thai")
    public String doiTrangThai(@PathVariable Long id, @RequestBody java.util.Map<String, Boolean> body) {
        DotGiamGia dgg = dotGiamGiaService.detailDotGiamGia(id);
        dgg.setTrangThai(body.get("trangThai"));
        dotGiamGiaService.updateDotGiamGia(dgg);
        return "ok";
    }
}