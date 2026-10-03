package com.example.datn.repository;

import com.example.datn.entity.HoaDon;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {

    boolean existsByMaHoaDon(String maHoaDon);

    boolean existsByMaHoaDonAndIdNot(String maHoaDon, Long id);

    @Query(
        """
        SELECT h FROM HoaDon h
        WHERE (:ma IS NULL OR LOWER(h.maHoaDon) LIKE LOWER(CONCAT('%', :ma, '%')))
          AND (:tuNgay IS NULL OR h.ngayTao >= :tuNgay)
          AND (:denNgay IS NULL OR h.ngayTao <= :denNgay)
          AND (:loaiDon IS NULL OR h.loaiDon = :loaiDon)
          AND (:trangThai IS NULL OR h.trangThai = :trangThai)
        """
    )
    Page<HoaDon> findByFilters(
        @Param("ma") String ma,
        @Param("tuNgay") LocalDateTime tuNgay,
        @Param("denNgay") LocalDateTime denNgay,
        @Param("loaiDon") String loaiDon,
        @Param("trangThai") String trangThai,
        Pageable pageable
    );
}
