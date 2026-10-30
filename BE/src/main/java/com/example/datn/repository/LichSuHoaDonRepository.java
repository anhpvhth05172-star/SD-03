package com.example.datn.repository;

import com.example.datn.entity.LichSuHoaDon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Long> {

    @Modifying
    @Query("DELETE FROM LichSuHoaDon l WHERE l.hoaDon.id = :idHoaDon")
    void deleteByHoaDonId(@Param("idHoaDon") Long idHoaDon);
}
