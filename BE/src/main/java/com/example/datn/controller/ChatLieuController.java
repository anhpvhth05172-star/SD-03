package com.example.datn.controller;

import com.example.datn.dto.AttributeRequest;
import com.example.datn.dto.AttributeResponse;
import com.example.datn.service.ChatLieuService;
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
@RequestMapping("/api/v1/chat-lieu")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class ChatLieuController {

    private final ChatLieuService chatLieuService;

    @GetMapping
    public ResponseEntity<List<AttributeResponse>> getAll(
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "activeOnly", required = false) Boolean activeOnly) {
        return ResponseEntity.ok(chatLieuService.getAll(search, activeOnly));
    }

    @GetMapping("/{id}")
    public ResponseEntity<AttributeResponse> getById(@PathVariable("id") Long id) {
        return ResponseEntity.ok(chatLieuService.getById(id));
    }

    @PostMapping
    public ResponseEntity<AttributeResponse> create(@Valid @RequestBody AttributeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(chatLieuService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<AttributeResponse> update(
            @PathVariable("id") Long id,
            @Valid @RequestBody AttributeRequest request) {
        return ResponseEntity.ok(chatLieuService.update(id, request));
    }

    @PatchMapping("/{id}/toggle-status")
    public ResponseEntity<AttributeResponse> toggleStatus(@PathVariable("id") Long id) {
        return ResponseEntity.ok(chatLieuService.toggleStatus(id));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") Long id) {
        chatLieuService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
