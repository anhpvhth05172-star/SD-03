package com.example.datn.repository;

import com.example.datn.entity.PhienDangNhap;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhienDangNhapRepository extends JpaRepository<PhienDangNhap, Long> {
    Optional<PhienDangNhap> findByRefreshTokenHash(String refreshTokenHash);
}
