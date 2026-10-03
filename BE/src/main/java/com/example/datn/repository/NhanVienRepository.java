package com.example.datn.repository;

import com.example.datn.entity.NhanVien;
import java.time.LocalDateTime;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface NhanVienRepository extends JpaRepository<NhanVien, Long> {

    boolean existsByMaNhanVien(String maNhanVien);

    boolean existsByMaNhanVienAndIdNot(String maNhanVien, Long id);

    boolean existsByTenTaiKhoan(String tenTaiKhoan);

    boolean existsByTenTaiKhoanAndIdNot(String tenTaiKhoan, Long id);

    @Query("SELECT COALESCE(MAX(n.id), 0) FROM NhanVien n")
    Long findMaxId();

    @Query(
        """
        SELECT n FROM NhanVien n
        WHERE (:keyword IS NULL
               OR LOWER(n.maNhanVien) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(n.tenTaiKhoan) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(n.email) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR n.soDienThoai LIKE CONCAT('%', :keyword, '%'))
          AND (:idVaiTro IS NULL OR n.vaiTro.id = :idVaiTro)
          AND (:trangThai IS NULL OR n.trangThai = :trangThai)
          AND (:tuNgay IS NULL OR n.ngayTao >= :tuNgay)
          AND (:denNgay IS NULL OR n.ngayTao <= :denNgay)
        """
    )
    Page<NhanVien> findByFilters(
        @Param("keyword") String keyword,
        @Param("idVaiTro") Long idVaiTro,
        @Param("trangThai") Boolean trangThai,
        @Param("tuNgay") LocalDateTime tuNgay,
        @Param("denNgay") LocalDateTime denNgay,
        Pageable pageable
    );
}