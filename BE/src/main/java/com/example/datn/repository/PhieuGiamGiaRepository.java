package com.example.datn.repository;

import com.example.datn.dto.PhieuGiamGiaDTO;
import com.example.datn.entity.PhieuGiamGia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface PhieuGiamGiaRepository extends JpaRepository<PhieuGiamGia, Long> {

    @Query("SELECT new com.example.datn.dto.PhieuGiamGiaDTO(pgg.id, pgg.maPhieuGiamGia, pgg.tenPhieuGiamGia, pgg.giaTriGiam, pgg.moTa, pgg.soLuongDaSuDung, pgg.ngayKetThuc, pgg.trangThai) FROM PhieuGiamGia pgg")
    Page<PhieuGiamGiaDTO> getListPageDiscount(Pageable pageable);
}
