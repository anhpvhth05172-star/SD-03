package com.example.datn.repository;

import com.example.datn.entity.GioHangChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface GioHangChiTietRepository extends JpaRepository<GioHangChiTiet, Integer> {

    Optional<GioHangChiTiet> findByGioHangIdAndSanPhamChiTietId(Integer idGioHang, Long idSanPhamChiTiet);

    Optional<GioHangChiTiet> findByIdAndGioHangKhachHangId(Integer id, Long idKhachHang);

    List<GioHangChiTiet> findByGioHangIdOrderByNgayThemDesc(Integer idGioHang);

    @Query("SELECT COALESCE(SUM(g.soLuong), 0) FROM GioHangChiTiet g WHERE g.gioHang.id = :idGioHang")
    int tongSoLuongTheoGioHang(@Param("idGioHang") Integer idGioHang);

    @Modifying
    @Query("DELETE FROM GioHangChiTiet g WHERE g.gioHang.id = :idGioHang")
    void xoaTatCaTheoGioHang(@Param("idGioHang") Integer idGioHang);

    @Modifying
    @Transactional
    void deleteByGioHang_KhachHang_IdAndSanPhamChiTiet_Id(Long khachHangId, Long sanPhamChiTietId);
}
