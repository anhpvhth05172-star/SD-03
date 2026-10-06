package com.example.datn.controller;

import com.example.datn.entity.DotGiamGia;
import com.example.datn.entity.PhieuGiamGia;
import com.example.datn.service.DotGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin/dot-giam-gia")
@CrossOrigin("*")
public class DotGiamGiaRestController {

    @Autowired
    private DotGiamGiaService dotGiamGiaService;

    @GetMapping
    public Page<DotGiamGia> getList(@RequestParam(name = "page", defaultValue = "0") Integer page,
                                    @RequestParam(name = "size", defaultValue = "8") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return dotGiamGiaService.getPage(pageable);
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
}