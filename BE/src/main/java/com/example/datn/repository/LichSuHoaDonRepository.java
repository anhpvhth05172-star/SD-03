package com.example.datn.repository;

import com.example.datn.entity.LichSuHoaDon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichSuHoaDonRepository extends JpaRepository<LichSuHoaDon, Long> {

    @Query("select l from LichSuHoaDon l where l.hoaDon.id = :idHoaDon order by l.thoiGian desc, l.id desc")
    List<LichSuHoaDon> findByIdHoaDonOrderByThoiGianDescIdDesc(@Param("idHoaDon") Long idHoaDon);

    long countByHoaDonIdAndTrangThai(@Param("hoaDonId") Long hoaDonId, @Param("trangThai") String trangThai);

    @Modifying
    @Query("DELETE FROM LichSuHoaDon l WHERE l.hoaDon.id = :idHoaDon")
    void deleteByHoaDonId(@Param("idHoaDon") Long idHoaDon);
}
