package com.example.datn.repository;

import com.example.datn.entity.ChiTietHoaDon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChiTietHoaDonRepository extends JpaRepository<ChiTietHoaDon, Long> {

    List<ChiTietHoaDon> findByHoaDonId(Long idHoaDon);

    @Modifying
    @Query("DELETE FROM ChiTietHoaDon c WHERE c.hoaDon.id = :idHoaDon")
    void deleteByHoaDonId(@Param("idHoaDon") Long idHoaDon);
}
