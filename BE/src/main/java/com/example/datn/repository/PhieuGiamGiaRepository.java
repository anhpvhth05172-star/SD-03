package com.example.datn.repository;

import com.example.datn.dto.PhieuGiamGiaDTO;
import com.example.datn.entity.PhieuGiamGia;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT new com.example.datn.dto.PhieuGiamGiaDTO(pgg.id, pgg.maPhieuGiamGia, pgg.tenPhieuGiamGia, pgg.giaTriGiam, pgg.moTa, pgg.soLuongDaSuDung, pgg.ngayKetThuc, pgg.trangThai) FROM PhieuGiamGia pgg")
    Page<PhieuGiamGiaDTO> getListPageDiscount(Pageable pageable);
}
