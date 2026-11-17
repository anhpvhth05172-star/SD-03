package com.example.datn.controller;

import com.example.datn.dto.LichLamViec.CaLamDTO;
import com.example.datn.dto.LichLamViec.CaLamRequest;
import com.example.datn.service.CaLamService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ca-lam")
@RequiredArgsConstructor
public class CaLamController {

    private final CaLamService caLamService;

    @GetMapping
    public ResponseEntity<List<CaLamDTO>> list() {
        return ResponseEntity.ok(caLamService.list());
    }

    @PostMapping
    public ResponseEntity<CaLamDTO> create(@RequestBody CaLamRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(caLamService.create(req));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CaLamDTO> update(@PathVariable Long id, @RequestBody CaLamRequest req) {
        return ResponseEntity.ok(caLamService.update(id, req));
    }

    @PutMapping("/{id}/trang-thai")
    public ResponseEntity<CaLamDTO> toggle(@PathVariable Long id) {
        return ResponseEntity.ok(caLamService.toggle(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        caLamService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
