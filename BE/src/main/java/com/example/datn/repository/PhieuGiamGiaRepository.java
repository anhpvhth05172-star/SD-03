package com.example.datn.repository;

import com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaDTO;
import com.example.datn.entity.PhieuGiamGia;
import jakarta.persistence.LockModeType;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from PhieuGiamGia p where p.id = :id")
    Optional<PhieuGiamGia> findByIdForUpdate(@Param("id") Long id);

    @Query("select p from PhieuGiamGia p left join fetch p.dotGiamGia")
    List<PhieuGiamGia> findAllWithDot();

    long countByDotGiamGiaId(Long idDotGiamGia);

    @Query("SELECT new com.example.datn.dto.PhieuGiamGia.PhieuGiamGiaDTO(pgg.id, pgg.maPhieuGiamGia, pgg.tenPhieuGiamGia, pgg.giaTriGiam, pgg.moTa, pgg.soLuongDaSuDung, pgg.ngayKetThuc, pgg.trangThai) FROM PhieuGiamGia pgg")
    Page<PhieuGiamGiaDTO> getListPageDiscount(Pageable pageable);

    boolean existsByMaPhieuGiamGiaAndIdNot(String ma, Long id);

        @Query("""
        SELECT p FROM PhieuGiamGia p
        WHERE (LOWER(p.maPhieuGiamGia) LIKE :pattern
               OR LOWER(p.tenPhieuGiamGia) LIKE :pattern)
          AND (:loaiGiamGia = '' OR p.loaiGiamGia = :loaiGiamGia)
          AND (:phamVi = '' OR p.phamVi = :phamVi)
          AND (:trangThai = ''
               OR (:trangThai = 'inactive' AND p.trangThai = false)
               OR (:trangThai = 'expired'  AND p.trangThai = true AND p.ngayKetThuc < :now)
               OR (:trangThai = 'upcoming' AND p.trangThai = true AND p.ngayBatDau > :now)
               OR (:trangThai = 'active'   AND p.trangThai = true
                   AND p.ngayBatDau <= :now AND p.ngayKetThuc >= :now))
          AND p.ngayBatDau >= :tuNgay
          AND p.ngayKetThuc < :denNgay
        """)
        Page<PhieuGiamGia> search(
                @Param("pattern") String pattern,
                @Param("loaiGiamGia") String loaiGiamGia,
                @Param("phamVi") String phamVi,
                @Param("trangThai") String trangThai,
                @Param("now") LocalDateTime now,
                @Param("tuNgay") LocalDateTime tuNgay,
                @Param("denNgay") LocalDateTime denNgay,
                Pageable pageable);
}
