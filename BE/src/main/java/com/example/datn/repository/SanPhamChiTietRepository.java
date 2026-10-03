package com.example.datn.repository;

import com.example.datn.entity.SanPhamChiTiet;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Long> {

    List<SanPhamChiTiet> findBySanPhamId(Long idSanPham);

    @Query("SELECT spct FROM SanPhamChiTiet spct " +
           "LEFT JOIN FETCH spct.sanPham sp " +
           "LEFT JOIN FETCH spct.mauSac ms " +
           "LEFT JOIN FETCH spct.kichCo kc " +
           "WHERE (:search IS NULL OR LOWER(spct.maChiTietSanPham) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(sp.tenSanPham) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(sp.maSanPham) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(ms.tenMau) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "   OR LOWER(kc.tenKichCo) LIKE LOWER(CONCAT('%', :search, '%'))) " +
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
