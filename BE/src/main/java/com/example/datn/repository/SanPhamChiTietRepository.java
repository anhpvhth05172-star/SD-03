package com.example.datn.repository;

import com.example.datn.entity.SanPhamChiTiet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Long> {

    List<SanPhamChiTiet> findBySanPhamId(Long idSanPham);

    boolean existsByMaChiTietSanPham(String maChiTietSanPham);

    boolean existsBySanPhamIdAndMauSacIdAndKichCoId(Long idSanPham, Long idMauSac, Long idKichCo);

    boolean existsBySanPhamIdAndMauSacIdAndKichCoIdAndIdNot(Long idSanPham, Long idMauSac, Long idKichCo, Long id);

    @Modifying
    @Query("DELETE FROM SanPhamChiTiet s WHERE s.sanPham.id = :idSanPham")
    void deleteBySanPhamId(@Param("idSanPham") Long idSanPham);


    @Query(value = "SELECT spct FROM SanPhamChiTiet spct " +
           "LEFT JOIN spct.sanPham sp " +
           "LEFT JOIN spct.mauSac ms " +
           "LEFT JOIN spct.kichCo kc " +
           "WHERE (:search IS NULL OR :search = '' OR spct.maChiTietSanPham LIKE CONCAT('%', :search, '%') " +
           "   OR sp.tenSanPham LIKE CONCAT('%', :search, '%') " +
           "   OR sp.maSanPham LIKE CONCAT('%', :search, '%') " +
           "   OR ms.tenMau LIKE CONCAT('%', :search, '%') " +
           "   OR kc.tenKichCo LIKE CONCAT('%', :search, '%')) " +
           "AND (:idSanPham IS NULL OR sp.id = :idSanPham) " +
           "AND (:idMauSac IS NULL OR ms.id = :idMauSac) " +
           "AND (:idKichCo IS NULL OR kc.id = :idKichCo) " +
           "AND (:trangThai IS NULL OR spct.trangThai = :trangThai)",
           countQuery = "SELECT COUNT(spct.id) FROM SanPhamChiTiet spct " +
           "LEFT JOIN spct.sanPham sp " +
           "LEFT JOIN spct.mauSac ms " +
           "LEFT JOIN spct.kichCo kc " +
           "WHERE (:search IS NULL OR :search = '' OR spct.maChiTietSanPham LIKE CONCAT('%', :search, '%') " +
           "   OR sp.tenSanPham LIKE CONCAT('%', :search, '%') " +
           "   OR sp.maSanPham LIKE CONCAT('%', :search, '%') " +
           "   OR ms.tenMau LIKE CONCAT('%', :search, '%') " +
           "   OR kc.tenKichCo LIKE CONCAT('%', :search, '%')) " +
           "AND (:idSanPham IS NULL OR sp.id = :idSanPham) " +
           "AND (:idMauSac IS NULL OR ms.id = :idMauSac) " +
           "AND (:idKichCo IS NULL OR kc.id = :idKichCo) " +
           "AND (:trangThai IS NULL OR spct.trangThai = :trangThai)")
    Page<SanPhamChiTiet> filterChiTiet(
            @Param("search") String search,
            @Param("idSanPham") Long idSanPham,
            @Param("idMauSac") Long idMauSac,
            @Param("idKichCo") Long idKichCo,
            @Param("trangThai") Boolean trangThai,
            Pageable pageable
    );
}
