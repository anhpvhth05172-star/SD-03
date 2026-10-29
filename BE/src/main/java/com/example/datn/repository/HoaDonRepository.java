package com.example.datn.repository;

import com.example.datn.entity.HoaDon;
import jakarta.persistence.LockModeType;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HoaDonRepository extends JpaRepository<HoaDon, Long> {

    @Query("""
        SELECT COALESCE(SUM(h.tienSauGiamGia - COALESCE(h.phiVanChuyen, 0)), 0) FROM HoaDon h
        WHERE h.daXoa = false AND h.trangThai IN :trangThais
          AND h.ngayTao >= :tu AND h.ngayTao < :den
        """)
    java.math.BigDecimal tongDoanhThuTheoTrangThai(
        @Param("trangThais") java.util.Collection<String> trangThais,
        @Param("tu") LocalDateTime tu,
        @Param("den") LocalDateTime den
    );

    boolean existsByMaHoaDon(String maHoaDon);

    boolean existsByMaHoaDonAndIdNot(String maHoaDon, Long id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HoaDon h WHERE h.id = :id")
    Optional<HoaDon> findByIdForUpdate(@Param("id") Long id);

    @Query(
        value = """
            SELECT * FROM hoa_don h
            WHERE (:ma IS NULL OR h.ma_hoa_don LIKE CONCAT('%', :ma, '%') OR h.ten_khach_hang LIKE CONCAT('%', :ma, '%') OR h.so_dien_thoai_khach_hang LIKE CONCAT('%', :ma, '%'))
              AND (:tuNgay IS NULL OR h.ngay_tao >= :tuNgay)
              AND (:denNgay IS NULL OR h.ngay_tao <= :denNgay)
              AND (:loaiDon IS NULL OR
          (:loaiDon = 'ONLINE' AND (h.loai_don = 'ONLINE' OR h.loai_don = N'Online')) OR
          (:loaiDon = 'TAI_QUAY_GIAO_HANG' AND (h.loai_don = 'TAI_QUAY_GIAO_HANG' OR (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND h.dia_chi_nhan_hang IS NOT NULL AND TRIM(h.dia_chi_nhan_hang) <> ''))) OR
          (:loaiDon = 'TAI_QUAY' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND (h.dia_chi_nhan_hang IS NULL OR TRIM(h.dia_chi_nhan_hang) = ''))))
              AND (:trangThai IS NULL OR h.trang_thai = :trangThai)
              AND h.da_xoa = :daXoa
            ORDER BY CASE WHEN h.trang_thai IN ('DA_HOAN_THANH', 'DA_HOAN_TIEN') THEN 1 ELSE 0 END ASC,
                     h.ngay_tao DESC, h.id DESC
            """,
        nativeQuery = true
    )
    List<HoaDon> findAllByFilters(
        @Param("ma") String ma,
        @Param("tuNgay") LocalDateTime tuNgay,
        @Param("denNgay") LocalDateTime denNgay,
        @Param("loaiDon") String loaiDon,
        @Param("trangThai") String trangThai,
        @Param("daXoa") boolean daXoa
    );

    @Query(
        value = """
            SELECT * FROM hoa_don h
            WHERE (:ma IS NULL OR h.ma_hoa_don LIKE CONCAT('%', :ma, '%') OR h.ten_khach_hang LIKE CONCAT('%', :ma, '%') OR h.so_dien_thoai_khach_hang LIKE CONCAT('%', :ma, '%'))
              AND (:tuNgay IS NULL OR h.ngay_tao >= :tuNgay)
              AND (:denNgay IS NULL OR h.ngay_tao <= :denNgay)
              AND (:loaiDon IS NULL OR
          (:loaiDon = 'ONLINE' AND (h.loai_don = 'ONLINE' OR h.loai_don = N'Online')) OR
          (:loaiDon = 'TAI_QUAY_GIAO_HANG' AND (h.loai_don = 'TAI_QUAY_GIAO_HANG' OR (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND h.dia_chi_nhan_hang IS NOT NULL AND TRIM(h.dia_chi_nhan_hang) <> ''))) OR
          (:loaiDon = 'TAI_QUAY' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND (h.dia_chi_nhan_hang IS NULL OR TRIM(h.dia_chi_nhan_hang) = ''))))
              AND (:trangThai IS NULL OR h.trang_thai = :trangThai)
              AND h.da_xoa = :daXoa
            ORDER BY CASE WHEN h.trang_thai IN ('DA_HOAN_THANH', 'DA_HOAN_TIEN') THEN 1 ELSE 0 END ASC,
                     h.ngay_tao DESC, h.id DESC
            """,
        countQuery = """
            SELECT COUNT(*) FROM hoa_don h
            WHERE (:ma IS NULL OR h.ma_hoa_don LIKE CONCAT('%', :ma, '%') OR h.ten_khach_hang LIKE CONCAT('%', :ma, '%') OR h.so_dien_thoai_khach_hang LIKE CONCAT('%', :ma, '%'))
              AND (:tuNgay IS NULL OR h.ngay_tao >= :tuNgay)
              AND (:denNgay IS NULL OR h.ngay_tao <= :denNgay)
              AND (:loaiDon IS NULL OR
          (:loaiDon = 'ONLINE' AND (h.loai_don = 'ONLINE' OR h.loai_don = N'Online')) OR
          (:loaiDon = 'TAI_QUAY_GIAO_HANG' AND (h.loai_don = 'TAI_QUAY_GIAO_HANG' OR (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND h.dia_chi_nhan_hang IS NOT NULL AND TRIM(h.dia_chi_nhan_hang) <> ''))) OR
          (:loaiDon = 'TAI_QUAY' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND (h.dia_chi_nhan_hang IS NULL OR TRIM(h.dia_chi_nhan_hang) = ''))))
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
