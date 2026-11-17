package com.example.datn.repository;

import com.example.datn.entity.GioHang;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GioHangRepository extends JpaRepository<GioHang, Integer> {

    Optional<GioHang> findFirstByKhachHangIdAndTrangThaiOrderByIdDesc(Long id, String trangThaiGioHang);
}
