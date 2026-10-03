package com.example.datn.controller;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.service.AttributeService;
import com.example.datn.service.ChatLieuService;
import com.example.datn.service.DeGiayService;
import com.example.datn.service.KichCoService;
import com.example.datn.service.KieuDangService;
import com.example.datn.service.LoaiGiayService;
import com.example.datn.service.MauSacService;
import com.example.datn.service.ThanGiayService;
import com.example.datn.service.ThuongHieuService;
import com.example.datn.service.XuatXuService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/attributes")
@RequiredArgsConstructor
@CrossOrigin(originPatterns = "*")
public class AttributeCategoryController {

    private final ThuongHieuService thuongHieuService;
    private final XuatXuService xuatXuService;
    private final ChatLieuService chatLieuService;
    private final KieuDangService kieuDangService;
    private final LoaiGiayService loaiGiayService;
    private final KichCoService kichCoService;
    private final MauSacService mauSacService;
    private final ThanGiayService thanGiayService;
    private final DeGiayService deGiayService;

    private AttributeService getService(String category) {
        String cat = category == null ? "" : category.trim().toLowerCase().replace("-", "_");
        return switch (cat) {
            case "thuong_hieu" -> thuongHieuService;
            case "xuat_xu" -> xuatXuService;
            case "chat_lieu" -> chatLieuService;
            case "kieu_dang" -> kieuDangService;
            case "loai_giay" -> loaiGiayService;
            case "kich_co" -> kichCoService;
            case "mau_sac" -> mauSacService;
            case "than_giay" -> thanGiayService;
            case "de_giay" -> deGiayService;
            default -> throw new IllegalArgumentException("Không hỗ trợ thuộc tính: " + category);
        };
    }

    @GetMapping("/{category}")
    public ResponseEntity<List<AttributeResponse>> getAll(
            @PathVariable("category") String category,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activeOnly", required = false) Boolean activeOnly) {
        return ResponseEntity.ok(getService(category).getAll(search, activeOnly));
    }

    @GetMapping("/{category}/{id}")
    public ResponseEntity<AttributeResponse> getById(
            @PathVariable("category") String category,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(getService(category).getById(id));
    }

    @PostMapping("/{category}")
    public ResponseEntity<AttributeResponse> create(
            @PathVariable("category") String category,
            @Valid @RequestBody AttributeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(getService(category).create(request));
    }

    @PutMapping("/{category}/{id}")
    public ResponseEntity<AttributeResponse> update(
            @PathVariable("category") String category,
            @PathVariable("id") Long id,
            @Valid @RequestBody AttributeRequest request) {
        return ResponseEntity.ok(getService(category).update(id, request));
    }

    @PatchMapping("/{category}/{id}/toggle-status")
    public ResponseEntity<AttributeResponse> toggleStatus(
            @PathVariable("category") String category,
            @PathVariable("id") Long id) {
        return ResponseEntity.ok(getService(category).toggleStatus(id));
    }

    @DeleteMapping("/{category}/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable("category") String category,
            @PathVariable("id") Long id) {
        getService(category).delete(id);
        return ResponseEntity.noContent().build();
    }
}
