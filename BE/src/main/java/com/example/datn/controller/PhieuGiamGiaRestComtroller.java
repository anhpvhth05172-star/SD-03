package com.example.datn.controller;

import com.example.datn.dto.PhieuGiamGiaDTO;
import com.example.datn.service.PhieuGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/phieu-giam-gia")
@CrossOrigin("*")
public class PhieuGiamGiaRestComtroller {

    @Autowired
    private PhieuGiamGiaService phieuGiamGiaService;

    @GetMapping
    public Page<PhieuGiamGiaDTO> getList(@RequestParam(name = "page", defaultValue = "0") Integer page,
                                         @RequestParam(name = "size", defaultValue = "8") Integer size) {
        Pageable pageable = PageRequest.of(page, size);
        return phieuGiamGiaService.getPageList(pageable);
    }
}
