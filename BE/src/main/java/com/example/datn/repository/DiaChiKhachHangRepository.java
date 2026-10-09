package com.example.datn.repository;

import com.example.datn.entity.DiaChiKhachHang;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DiaChiKhachHangRepository extends JpaRepository<DiaChiKhachHang, Long> {

    List<DiaChiKhachHang> findByKhachHangIdOrderByMacDinhDescIdDesc(Long idKhachHang);

    Optional<DiaChiKhachHang> findFirstByKhachHangIdAndMacDinhTrue(Long idKhachHang);

    long countByKhachHangId(Long idKhachHang);
}