package com.example.datn.controller;

import com.example.datn.dto.CaLamViecDto;
import com.example.datn.dto.KetCaRequest;
import com.example.datn.dto.MoCaRequest;
import com.example.datn.dto.TongKetDto;
import com.example.datn.service.GiaoCaService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/ca")
@RequiredArgsConstructor
public class GiaoCaController {

    private final GiaoCaService giaoCaService;

    @PostMapping("/mo-ca")
    public ResponseEntity<CaLamViecDto> moCa(@RequestBody MoCaRequest request, Authentication auth) {
        idNhanVien(auth);
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(giaoCaService.moCa(request.getTienDauCa(), request.getGhiChu()));
    }

    @GetMapping("/dang-mo")
    public CaLamViecDto dangMo(Authentication auth) {
        return giaoCaService.getDangMoOf(idNhanVien(auth));
    }

    @GetMapping("/{id}/tong-ket")
    public TongKetDto tongKet(@PathVariable Long id) {
        return giaoCaService.tongKet(id);
    }

    @PostMapping("/{id}/ket-ca")
    public CaLamViecDto ketCa(
        @PathVariable Long id,
        @RequestBody KetCaRequest request,
        Authentication auth
    ) {
        idNhanVien(auth);
        return giaoCaService.ketCa(id, request.getTienThucTe(), request.getLyDoLech(), request.getGhiChu());
    }

    @GetMapping("/{id}/doi-soat")
    public CaLamViecDto doiSoat(@PathVariable Long id, Authentication auth) {
        idNhanVien(auth);
        return giaoCaService.doiSoat(id);
    }

    @GetMapping("/lich-su")
    public org.springframework.data.domain.Page<CaLamViecDto> lichSu(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "10") int size,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tuNgay,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate denNgay,
        @RequestParam(required = false) String trangThai,
        @RequestParam(required = false) Long idNhanVien,
        Authentication auth
    ) {
        Long callerId = idUser(auth);
        boolean callerAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> new SimpleGrantedAuthority("ROLE_ADMIN").equals(a));
        return giaoCaService.lichSu(page, size, tuNgay, denNgay, trangThai, idNhanVien, callerId, callerAdmin);
    }

    private static Long idNhanVien(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        boolean laNV = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_NHAN_VIEN".equals(a.getAuthority()));
        if (!laNV) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cần đăng nhập nhân viên");
        }
        return id;
    }

    private static Long idUser(Authentication auth) {
        if (auth == null || !(auth.getPrincipal() instanceof Long id)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Chưa đăng nhập");
        }
        boolean laNVHoacAdmin = auth.getAuthorities().stream()
            .anyMatch(a -> "ROLE_NHAN_VIEN".equals(a.getAuthority()) || "ROLE_ADMIN".equals(a.getAuthority()));
        if (!laNVHoacAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Cần đăng nhập nhân viên hoặc admin");
        }
        return id;
    }
}
