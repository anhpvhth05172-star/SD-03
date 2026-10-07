package com.example.datn.repository;

import com.example.datn.entity.DotGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;

public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Long> {

    @Query("""
                SELECT d FROM DotGiamGia d
                WHERE (:ten IS NULL OR LOWER(d.tenDotGiamGia) LIKE LOWER(CONCAT('%', :ten, '%')))
                  AND (:trangThai IS NULL OR d.trangThai = :trangThai)
                  AND (:from IS NULL OR d.ngayBatDau >= :from)
                  AND (:to IS NULL OR d.ngayKetThuc < :to)
                ORDER BY d.id DESC
            """)
    Page<DotGiamGia> search(@Param("ten") String ten,
                            @Param("trangThai") Boolean trangThai,
                            @Param("from") LocalDateTime from,
                            @Param("to") LocalDateTime to,
                            Pageable pageable);
}
