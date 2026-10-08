package com.example.datn.repository;

import com.example.datn.dto.KhachHangThongKeDTO;
import com.example.datn.entity.KhachHang;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface KhachHangRepository extends JpaRepository<KhachHang, Long> {

    boolean existsByMaKhachHang(String maKhachHang);

    boolean existsByMaKhachHangAndIdNot(String maKhachHang, Long id);

    boolean existsByTenTaiKhoan(String tenTaiKhoan);

    boolean existsByTenTaiKhoanAndIdNot(String tenTaiKhoan, Long id);

    boolean existsByEmail(String email);

    boolean existsByEmailAndIdNot(String email, Long id);

    @Query("select k from KhachHang k where k.email = :taiKhoan or k.tenTaiKhoan = :taiKhoan")
    Optional<KhachHang> findByEmailOrTenTaiKhoan(@Param("taiKhoan") String taiKhoan);

    @Query("SELECT COALESCE(MAX(k.id), 0) FROM KhachHang k")
    Long findMaxId();

    @Query(
        """
        SELECT k FROM KhachHang k
        WHERE (:keyword IS NULL
               OR LOWER(k.maKhachHang) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(k.tenKhachHang) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(k.tenTaiKhoan) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(k.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR k.soDienThoai LIKE CONCAT('%', :keyword, '%'))
          AND (:trangThai IS NULL OR k.trangThai = :trangThai)
          AND (:tuNgay IS NULL OR k.ngayTao >= :tuNgay)
          AND (:denNgay IS NULL OR k.ngayTao <= :denNgay)
          AND (:tongChiTieuMin IS NULL
               OR (SELECT COALESCE(SUM(h.tienSauGiamGia), 0) FROM HoaDon h
                   WHERE h.khachHang = k AND h.trangThai NOT IN ('DA_HUY', 'DA_HOAN_TIEN')) >= :tongChiTieuMin)
          AND (:tongChiTieuMax IS NULL
               OR (SELECT COALESCE(SUM(h.tienSauGiamGia), 0) FROM HoaDon h
                   WHERE h.khachHang = k AND h.trangThai NOT IN ('DA_HUY', 'DA_HOAN_TIEN')) < :tongChiTieuMax)
        """
    )
    Page<KhachHang> findByFilters(
        @Param("keyword") String keyword,
        @Param("trangThai") Boolean trangThai,
        @Param("tuNgay") LocalDateTime tuNgay,
        @Param("denNgay") LocalDateTime denNgay,
        @Param("tongChiTieuMin") BigDecimal tongChiTieuMin,
        @Param("tongChiTieuMax") BigDecimal tongChiTieuMax,
        Pageable pageable
    );

    @Query(
        """
        SELECT new com.example.datn.dto.KhachHangThongKeDTO(
            h.khachHang.id, COUNT(h.id), SUM(h.tienSauGiamGia)
        )
        FROM HoaDon h
        WHERE h.khachHang.id IN :ids
          AND h.trangThai NOT IN ('DA_HUY', 'DA_HOAN_TIEN')
        GROUP BY h.khachHang.id
        """
    )
    List<KhachHangThongKeDTO> thongKeTheoKhachHang(@Param("ids") List<Long> ids);
}
