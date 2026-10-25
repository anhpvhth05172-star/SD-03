package com.example.datn.repository;

import com.example.datn.entity.DotGiamGiaChiTiet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
@Repository
public interface DotGiamGiaChiTietRepository extends JpaRepository<DotGiamGiaChiTiet, Long> {

    // Biến thể đã nằm trong đợt khác có thời gian chồng lấn
    @Query("""
        select ct from DotGiamGiaChiTiet ct
        join fetch ct.dotGiamGia d
        join fetch ct.sanPhamChiTiet spct
        where spct.id in :ids
          and ct.trangThai = true and d.trangThai = true
          and d.ngayBatDau < :ketThuc and d.ngayKetThuc > :batDau
        """)
    List<DotGiamGiaChiTiet> findOverlapping(
            @Param("ids") Collection<Long> ids,
            @Param("batDau") LocalDateTime batDau,
            @Param("ketThuc") LocalDateTime ketThuc);

    // Đợt đang hiệu lực của các biến thể (dùng để tính giá hiển thị)
    @Query("""
        select ct from DotGiamGiaChiTiet ct
        join fetch ct.dotGiamGia d
        where ct.sanPhamChiTiet.id in :ids
          and ct.trangThai = true and d.trangThai = true
          and d.ngayBatDau <= :now and d.ngayKetThuc >= :now
        """)
    List<DotGiamGiaChiTiet> findActiveByVariantIds(
            @Param("ids") Collection<Long> ids,
            @Param("now") LocalDateTime now);

    @Query("""
    select ct from DotGiamGiaChiTiet ct
    join fetch ct.sanPhamChiTiet spct
    join fetch spct.sanPham sp
    join fetch spct.kichCo
    join fetch spct.mauSac
    where ct.dotGiamGia.id = :idDot
    order by sp.tenSanPham asc, spct.maChiTietSanPham asc
    """)
    List<DotGiamGiaChiTiet> findByDotId(@Param("idDot") Long idDot);

    @Query("""
    select ct.sanPhamChiTiet.id, d.tenDotGiamGia, d.phanTramGiam
    from DotGiamGiaChiTiet ct
    join ct.dotGiamGia d
    where ct.trangThai = true
      and d.trangThai = true
      and d.ngayBatDau <= :now
      and d.ngayKetThuc >= :now
      and d.id <> :excludeId
""")
    List<Object[]> findApDungHienTai(@Param("now") LocalDateTime now,
                                     @Param("excludeId") Long excludeId);
}
