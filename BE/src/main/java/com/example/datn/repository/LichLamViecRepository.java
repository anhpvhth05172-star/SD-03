package com.example.datn.repository;

import com.example.datn.entity.LichLamViec;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {
    @Query("SELECT l FROM LichLamViec l JOIN FETCH l.caLamViec c WHERE l.nhanVien.id = :idNhanVien AND l.ngay = :ngay AND c.dangBat = true ORDER BY c.batDau")
    List<LichLamViec> findCaDangBatDuocXep(@Param("idNhanVien") Long idNhanVien, @Param("ngay") LocalDate ngay);
    List<LichLamViec> findAllByOrderByNgayAscIdAsc();
    boolean existsByNhanVienIdAndCaLamViecIdAndNgay(Long idNhanVien, Long idCa, LocalDate ngay);
    boolean existsByNhanVienIdAndCaLamViecIdAndNgayAndIdNot(Long idNhanVien, Long idCa, LocalDate ngay, Long id);
    boolean existsByCaLamViecId(Long idCa);
    void deleteAllByNgay(LocalDate ngay);
}
