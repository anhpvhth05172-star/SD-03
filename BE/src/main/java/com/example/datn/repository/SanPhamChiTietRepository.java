package com.example.datn.repository;

import com.example.datn.entity.SanPhamChiTiet;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface SanPhamChiTietRepository extends JpaRepository<SanPhamChiTiet, Long> {

    boolean existsByMaChiTietSanPham(String maChiTietSanPham);

    List<SanPhamChiTiet> findBySanPhamId(Long idSanPham);

    @Query(
        """
        SELECT c.sanPham.id, COUNT(c), COALESCE(SUM(c.soLuong), 0), MIN(c.giaBan)
        FROM SanPhamChiTiet c
        GROUP BY c.sanPham.id
        """
    )
    List<Object[]> tongHopTheoSanPham();
}
