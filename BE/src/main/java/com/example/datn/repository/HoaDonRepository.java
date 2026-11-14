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
        SELECT h.nhanVien.id, COUNT(h)
        FROM HoaDon h
        WHERE h.nhanVien IS NOT NULL
          AND h.loaiDon = 'ONLINE'
          AND h.trangThai NOT IN ('DA_HOAN_THANH', 'DA_HUY', 'DA_HOAN_TIEN')
        GROUP BY h.nhanVien.id
        """)
    List<Object[]> countOpenOrdersByEmployee();

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

    Optional<HoaDon> findByMaHoaDon(String maHoaDon);

    @Query("""
        SELECT DISTINCT h FROM HoaDon h
        LEFT JOIN FETCH h.khachHang kh
        LEFT JOIN FETCH h.phuongThucThanhToan pt
        LEFT JOIN FETCH h.phieuGiamGia pgg
        LEFT JOIN FETCH h.dotGiamGia dgg
        LEFT JOIN FETCH h.nhanVien nv
        WHERE h.maHoaDon = :maHoaDon AND h.daXoa = false
        """)
    Optional<HoaDon> findByMaHoaDonPublic(@Param("maHoaDon") String maHoaDon);

    @Query("""
        SELECT DISTINCT h FROM HoaDon h
        LEFT JOIN FETCH h.khachHang kh
        LEFT JOIN FETCH h.phuongThucThanhToan pt
        LEFT JOIN FETCH h.phieuGiamGia pgg
        LEFT JOIN FETCH h.dotGiamGia dgg
        LEFT JOIN FETCH h.nhanVien nv
        WHERE h.daXoa = false
          AND (h.soDienThoaiKhachHang = :phone OR (kh IS NOT NULL AND kh.soDienThoai = :phone))
        ORDER BY h.ngayTao DESC, h.id DESC
        """)
    List<HoaDon> findByPhonePublic(@Param("phone") String phone);

    boolean existsByMaHoaDonAndIdNot(String maHoaDon, Long id);

    @Query("""
        SELECT h FROM HoaDon h
        WHERE h.khachHang.id = :idKhachHang
          AND h.daXoa = false
          AND (:keyword IS NULL OR LOWER(h.maHoaDon) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR h.soDienThoaiKhachHang LIKE CONCAT('%', :keyword, '%'))
        ORDER BY h.ngayTao DESC, h.id DESC
        """)
    Page<HoaDon> findOrdersForCustomer(
        @Param("idKhachHang") Long idKhachHang,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT h FROM HoaDon h WHERE h.id = :id")
    Optional<HoaDon> findByIdForUpdate(@Param("id") Long id);

    @Query("""
        SELECT h FROM HoaDon h
        WHERE h.loaiDon = 'TAI_QUAY'
          AND h.trangThai = 'CHO_XAC_NHAN'
          AND h.daXoa = false
        ORDER BY h.id ASC
        """)
    List<HoaDon> findPosWaitingOrders();

    @Query("""
        SELECT h FROM HoaDon h
        WHERE h.loaiDon = 'TAI_QUAY'
          AND h.trangThai = 'CHO_XAC_NHAN'
          AND h.daXoa = false
          AND h.nhanVien.id = :idNhanVien
        ORDER BY h.id ASC
        """)
    List<HoaDon> findPosWaitingOrdersByEmployee(@Param("idNhanVien") Long idNhanVien);

    @Query(
        value = """
            SELECT * FROM hoa_don h
            WHERE (:ma IS NULL OR h.ma_hoa_don LIKE CONCAT('%', :ma, '%') OR h.ten_khach_hang LIKE CONCAT('%', :ma, '%') OR h.so_dien_thoai_khach_hang LIKE CONCAT('%', :ma, '%'))
              AND (:tuNgay IS NULL OR h.ngay_tao >= :tuNgay)
              AND (:denNgay IS NULL OR h.ngay_tao <= :denNgay)
              AND (:loaiDon IS NULL OR
          (:loaiDon = 'ONLINE' AND (h.loai_don = 'ONLINE' OR h.loai_don = N'Online')) OR
          (:loaiDon = 'TAI_QUAY_GIAO_HANG' AND (h.loai_don = 'TAI_QUAY_GIAO_HANG' OR (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND h.dia_chi_nhan_hang IS NOT NULL AND TRIM(h.dia_chi_nhan_hang) <> ''))) OR
          (:loaiDon = 'TAI_QUAY' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND (h.dia_chi_nhan_hang IS NULL OR TRIM(h.dia_chi_nhan_hang) = ''))) OR
          (:loaiDon = 'TAI_QUAY_TAT_CA' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy', 'TAI_QUAY_GIAO_HANG'))))
              AND ((:trangThai IS NULL AND h.trang_thai NOT IN ('DA_HUY', N'Đã hủy')) OR
                   (:trangThai IS NOT NULL AND (h.trang_thai = :trangThai OR (:trangThai = 'DA_HUY' AND h.trang_thai IN ('DA_HUY', N'Đã hủy')))))
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
          (:loaiDon = 'TAI_QUAY' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND (h.dia_chi_nhan_hang IS NULL OR TRIM(h.dia_chi_nhan_hang) = ''))) OR
          (:loaiDon = 'TAI_QUAY_TAT_CA' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy', 'TAI_QUAY_GIAO_HANG'))))
              AND ((:trangThai IS NULL AND h.trang_thai NOT IN ('DA_HUY', N'Đã hủy')) OR
                   (:trangThai IS NOT NULL AND (h.trang_thai = :trangThai OR (:trangThai = 'DA_HUY' AND h.trang_thai IN ('DA_HUY', N'Đã hủy')))))
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
          (:loaiDon = 'TAI_QUAY' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy') AND (h.dia_chi_nhan_hang IS NULL OR TRIM(h.dia_chi_nhan_hang) = ''))) OR
          (:loaiDon = 'TAI_QUAY_TAT_CA' AND (h.loai_don IN ('TAI_QUAY', N'Tại quầy', 'TAI_QUAY_GIAO_HANG'))))
              AND ((:trangThai IS NULL AND h.trang_thai NOT IN ('DA_HUY', N'Đã hủy')) OR
                   (:trangThai IS NOT NULL AND (h.trang_thai = :trangThai OR (:trangThai = 'DA_HUY' AND h.trang_thai IN ('DA_HUY', N'Đã hủy')))))
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

    List<HoaDon> findByTrangThaiAndNgayTaoBefore(String trangThai, LocalDateTime before);
}
