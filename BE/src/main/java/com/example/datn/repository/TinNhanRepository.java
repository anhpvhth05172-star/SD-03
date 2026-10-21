package com.example.datn.repository;

import com.example.datn.entity.TinNhan;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TinNhanRepository extends JpaRepository<TinNhan, Long> {

    @Query("SELECT t FROM TinNhan t WHERE t.hoiThoai.id = :idHoiThoai ORDER BY t.id ASC")
    List<TinNhan> timTheoHoiThoai(@Param("idHoiThoai") Long idHoiThoai);

    @Query("SELECT t FROM TinNhan t WHERE t.hoiThoai.id = :idHoiThoai AND t.id > :tuId ORDER BY t.id ASC")
    List<TinNhan> timMoiHon(@Param("idHoiThoai") Long idHoiThoai, @Param("tuId") Long tuId);

    @Modifying
    @Query("UPDATE TinNhan t SET t.daDoc = true WHERE t.hoiThoai.id = :idHoiThoai AND t.loaiNguoiGui = :loaiGui")
    void danhDaDoc(@Param("idHoiThoai") Long idHoiThoai, @Param("loaiGui") String loaiGui);

    @Query("SELECT COUNT(t) FROM TinNhan t WHERE t.hoiThoai.id = :idHoiThoai AND t.loaiNguoiGui = :loaiGui AND t.daDoc = false")
    long demChuaDoc(@Param("idHoiThoai") Long idHoiThoai, @Param("loaiGui") String loaiGui);
}
