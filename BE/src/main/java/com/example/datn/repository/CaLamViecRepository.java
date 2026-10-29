package com.example.datn.repository;

import com.example.datn.entity.CaLamViec;
import com.example.datn.entity.HoaDon;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaLamViecRepository extends JpaRepository<CaLamViec, Long> {

    Optional<CaLamViec> findFirstByNhanVienIdAndTrangThaiAndDaXoaFalse(Long idNhanVien, String trangThai);

    Optional<CaLamViec> findFirstByTrangThaiAndDaXoaFalse(String trangThai);

    Optional<CaLamViec> findByIdAndDaXoaFalse(Long id);

    @Query("""
        SELECT c FROM CaLamViec c
        LEFT JOIN c.nhanVien nv
        WHERE c.daXoa = false
          AND (:idNhanVien IS NULL OR c.nhanVien.id = :idNhanVien)
          AND (:trangThai IS NULL OR c.trangThai = :trangThai)
          AND (:tu IS NULL OR c.thoiGianMo >= :tu)
          AND (:den IS NULL OR c.thoiGianMo < :den)
          AND (:keyword IS NULL
               OR LOWER(c.maCa) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(nv.tenTaiKhoan) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(nv.maNhanVien) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(nv.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR EXISTS (
                   SELECT 1 FROM HoaDon h
                   WHERE h.idCaLamViec = c.id
                     AND LOWER(h.maHoaDon) LIKE LOWER(CONCAT('%', :keyword, '%'))
               ))
        ORDER BY c.thoiGianMo DESC
        """)
    Page<CaLamViec> lichSu(
        @Param("idNhanVien") Long idNhanVien,
        @Param("trangThai") String trangThai,
        @Param("tu") LocalDateTime tu,
        @Param("den") LocalDateTime den,
        @Param("keyword") String keyword,
        Pageable pageable
    );

    @Modifying(clearAutomatically = true)
    @Query("""
        UPDATE CaLamViec c
        SET c.trangThai = 'DA_KET_CA',
            c.thoiGianKet = :thoiGianKet,
            c.tongHoaDon = :tongHoaDon,
            c.tongDoanhThu = :tongDoanhThu,
            c.tienMatThu = :tienMatThu,
            c.tienKhongMat = :tienKhongMat,
            c.tienHoanMat = :tienHoanMat,
            c.tienDuKien = :tienDuKien,
            c.tienThucTe = :tienThucTe,
            c.chenhLech = :chenhLech,
            c.lyDoLech = :lyDoLech,
            c.ngayCapNhat = :ngayCapNhat,
            c.nguoiCapNhat = :nguoiCapNhat
        WHERE c.id = :id AND c.trangThai = 'DANG_MO' AND c.daXoa = false
        """)
    int ketCaConditional(
        @Param("id") Long id,
        @Param("thoiGianKet") LocalDateTime thoiGianKet,
        @Param("tongHoaDon") Integer tongHoaDon,
        @Param("tongDoanhThu") BigDecimal tongDoanhThu,
        @Param("tienMatThu") BigDecimal tienMatThu,
        @Param("tienKhongMat") BigDecimal tienKhongMat,
        @Param("tienHoanMat") BigDecimal tienHoanMat,
        @Param("tienDuKien") BigDecimal tienDuKien,
        @Param("tienThucTe") BigDecimal tienThucTe,
        @Param("chenhLech") BigDecimal chenhLech,
        @Param("lyDoLech") String lyDoLech,
        @Param("ngayCapNhat") LocalDateTime ngayCapNhat,
        @Param("nguoiCapNhat") String nguoiCapNhat
    );

    @Query("SELECT COUNT(h) FROM HoaDon h WHERE h.idCaLamViec = :idCa AND h.trangThai = 'DA_HOAN_THANH' AND h.loaiDon = 'TAI_QUAY'")
    int demHoaDonTaiQuayHoanThanh(@Param("idCa") Long idCa);

    @Query("SELECT COALESCE(SUM(h.tienSauGiamGia), 0) FROM HoaDon h WHERE h.idCaLamViec = :idCa AND h.trangThai = 'DA_HOAN_THANH'")
    BigDecimal tongDoanhThuHieuLuc(@Param("idCa") Long idCa);

    @Query("""
        SELECT COALESCE(SUM(h.tienSauGiamGia), 0) FROM HoaDon h
        JOIN h.phuongThucThanhToan p
        WHERE h.idCaLamViec = :idCa AND h.trangThai = 'DA_HOAN_THANH'
          AND p.tenPhuongThuc LIKE %:tuKhoa%
        """)
    BigDecimal tongTienMatHieuLuc(@Param("idCa") Long idCa, @Param("tuKhoa") String tuKhoa);

    @Query("""
        SELECT COALESCE(SUM(h.tienSauGiamGia), 0) FROM HoaDon h
        JOIN h.phuongThucThanhToan p
        WHERE h.idCaLamViec = :idCa AND h.trangThai = 'DA_HOAN_THANH'
          AND p.tenPhuongThuc NOT LIKE %:tuKhoa%
        """)
    BigDecimal tongTienKhongMatHieuLuc(@Param("idCa") Long idCa, @Param("tuKhoa") String tuKhoa);

    @Query("""
        SELECT COALESCE(SUM(h.tienSauGiamGia), 0) FROM HoaDon h
        JOIN h.phuongThucThanhToan p
        WHERE h.idCaLamViec = :idCa AND h.trangThai = 'DA_HOAN_TIEN'
          AND p.tenPhuongThuc LIKE %:tuKhoa%
        """)
    BigDecimal tongTienHoanMat(@Param("idCa") Long idCa, @Param("tuKhoa") String tuKhoa);

    @Query("""
        SELECT h FROM HoaDon h
        WHERE h.idCaLamViec = :idCa AND h.loaiDon = 'TAI_QUAY'
          AND h.trangThai NOT IN ('DA_HOAN_THANH', 'DA_HUY', 'DA_HOAN_TIEN')
          AND h.daXoa = false
        """)
    List<HoaDon> hoaDonDangXuLy(@Param("idCa") Long idCa);

    @Query("SELECT h FROM HoaDon h WHERE h.idCaLamViec = :idCa ORDER BY h.ngayTao DESC, h.id DESC")
    List<HoaDon> hoaDonGanDay(
        @Param("idCa") Long idCa,
        Pageable pageable
    );
}
