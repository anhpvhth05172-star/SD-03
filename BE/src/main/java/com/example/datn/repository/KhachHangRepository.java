package com.example.datn.repository;

import com.example.datn.entity.KhachHang;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    boolean existsByTenTaiKhoan(String tenTaiKhoan);

    boolean existsByEmail(String email);

    boolean existsByMaKhachHang(String maKhachHang);

    @Query("select k from KhachHang k where k.email = :taiKhoan or k.tenTaiKhoan = :taiKhoan")
    Optional<KhachHang> findByEmailOrTenTaiKhoan(@Param("taiKhoan") String taiKhoan);
}
