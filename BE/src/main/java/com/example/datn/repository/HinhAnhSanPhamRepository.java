package com.example.datn.repository;

import com.example.datn.entity.HinhAnhSanPham;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface HinhAnhSanPhamRepository extends JpaRepository<HinhAnhSanPham, Long> {

    List<HinhAnhSanPham> findBySanPhamId(Long idSanPham);

    @Modifying
    @Query("DELETE FROM HinhAnhSanPham h WHERE h.sanPham.id = :idSanPham")
    void deleteBySanPhamId(@Param("idSanPham") Long idSanPham);
}
