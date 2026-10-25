package com.example.datn.repository;

import com.example.datn.entity.LichLamViec;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {
    List<LichLamViec> findAllByOrderByNgayAscIdAsc();
    boolean existsByNhanVienIdAndCaLamViecIdAndNgay(Long idNhanVien, Long idCa, LocalDate ngay);
    boolean existsByNhanVienIdAndCaLamViecIdAndNgayAndIdNot(Long idNhanVien, Long idCa, LocalDate ngay, Long id);
    boolean existsByCaLamViecId(Long idCa);
    void deleteAllByNgay(LocalDate ngay);
}
