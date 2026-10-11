package com.example.datn.repository;

import com.example.datn.entity.KhachHangPhieuGiamGia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface KhachHangPhieuGiamGiaRepository extends JpaRepository<KhachHangPhieuGiamGia, Long> {

    @Query("""
        select r from KhachHangPhieuGiamGia r
        join fetch r.khachHang kh
        where r.phieuGiamGia.id = :idPhieu
        order by kh.tenKhachHang asc
        """)
    List<KhachHangPhieuGiamGia> findByPhieuId(@Param("idPhieu") Long idPhieu);


}
