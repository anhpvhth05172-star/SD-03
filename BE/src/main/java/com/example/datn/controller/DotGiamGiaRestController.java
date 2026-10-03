package com.example.datn.controller;

import com.example.datn.dto.PhieuGiamGiaDTO;
import com.example.datn.entity.DotGiamGia;
import com.example.datn.service.DotGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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
}
