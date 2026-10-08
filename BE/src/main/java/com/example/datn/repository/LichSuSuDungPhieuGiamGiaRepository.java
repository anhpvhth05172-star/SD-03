package com.example.datn.repository;

import com.example.datn.entity.LichSuSuDungPhieuGiamGia;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichSuSuDungPhieuGiamGiaRepository
    extends JpaRepository<LichSuSuDungPhieuGiamGia, Long> {

    long countByKhachHangIdAndPhieuGiamGiaId(Long idKhachHang, Long idPhieuGiamGia);

    boolean existsByHoaDonIdAndPhieuGiamGiaId(Long idHoaDon, Long idPhieuGiamGia);

    List<LichSuSuDungPhieuGiamGia> findByKhachHangId(Long idKhachHang);

    @Modifying
    @Query("delete from LichSuSuDungPhieuGiamGia l where l.hoaDon.id = :idHoaDon")
    void deleteByHoaDonId(@Param("idHoaDon") Long idHoaDon);

    @Query("""
        select l.phieuGiamGia.id, count(l)
        from LichSuSuDungPhieuGiamGia l
        where l.khachHang.id = :idKhachHang
        group by l.phieuGiamGia.id
        """)
    List<Object[]> demTheoPhieuCuaKhachHang(@Param("idKhachHang") Long idKhachHang);
}
