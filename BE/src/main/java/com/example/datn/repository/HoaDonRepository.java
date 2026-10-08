package com.example.datn.repository;

import com.example.datn.entity.HoaDon;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {

    boolean existsByMaHoaDon(String maHoaDon);

    boolean existsByMaHoaDonAndIdNot(String maHoaDon, Long id);

    @Query(
        value = """
            SELECT * FROM hoa_don h
            WHERE (:ma IS NULL OR h.ma_hoa_don LIKE CONCAT('%', :ma, '%') COLLATE Latin1_General_CS_AS)
              AND (:tuNgay IS NULL OR h.ngay_tao >= :tuNgay)
              AND (:denNgay IS NULL OR h.ngay_tao <= :denNgay)
              AND (:loaiDon IS NULL OR h.loai_don = :loaiDon)
              AND (:trangThai IS NULL OR h.trang_thai = :trangThai)
              AND h.da_xoa = :daXoa
            ORDER BY h.ngay_tao DESC, h.id DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM hoa_don h
            WHERE (:ma IS NULL OR h.ma_hoa_don LIKE CONCAT('%', :ma, '%') COLLATE Latin1_General_CS_AS)
              AND (:tuNgay IS NULL OR h.ngay_tao >= :tuNgay)
              AND (:denNgay IS NULL OR h.ngay_tao <= :denNgay)
              AND (:loaiDon IS NULL OR h.loai_don = :loaiDon)
              AND (:trangThai IS NULL OR h.trang_thai = :trangThai)
              AND h.da_xoa = :daXoa
            """,
        nativeQuery = true
    )
    Page<HoaDon> findByFilters(
        @Param("ma") String ma,
        @Param("tuNgay") LocalDateTime tuNgay,
        @Param("denNgay") LocalDateTime denNgay,
        @Param("loaiDon") String loaiDon,
        @Param("trangThai") String trangThai,
        @Param("daXoa") boolean daXoa,
        Pageable pageable
    );

    @Query(
        """
        SELECT h.khachHang.id, COUNT(h), COALESCE(SUM(h.tienSauGiamGia), 0)
        FROM HoaDon h
        WHERE h.khachHang IS NOT NULL AND h.daXoa = false
        GROUP BY h.khachHang.id
        """
    )
    List<Object[]> tongHopTheoKhachHang();
}
