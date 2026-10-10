package com.example.datn.repository;

import com.example.datn.entity.ChiTietHoaDon;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ChiTietHoaDonRepository extends JpaRepository<ChiTietHoaDon, Long> {

    List<ChiTietHoaDon> findByHoaDonId(Long idHoaDon);

    @Modifying
    @Query("DELETE FROM ChiTietHoaDon c WHERE c.hoaDon.id = :idHoaDon")
    void deleteByHoaDonId(@Param("idHoaDon") Long idHoaDon);

    boolean existsBySanPhamChiTietId(Long idSanPhamChiTiet);

    boolean existsBySanPhamChiTiet_SanPham_Id(Long idSanPham);

    @Query(value = """
        SELECT id_hoa_don, SUM(so_luong)
        FROM chi_tiet_hoa_don
        WHERE id_hoa_don IN (:ids)
        GROUP BY id_hoa_don
        """, nativeQuery = true)
    List<Object[]> tongSoLuongTheoHoaDon(@Param("ids") List<Long> ids);
}

