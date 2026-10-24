package com.example.datn.repository;

import com.example.datn.entity.DanhMucCaLamViec;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DanhMucCaLamViecRepository extends JpaRepository<DanhMucCaLamViec, Long> {
    List<DanhMucCaLamViec> findAllByOrderByTenAsc();
    boolean existsByMa(String ma);
    boolean existsByMaAndIdNot(String ma, Long id);
    boolean existsByTenIgnoreCase(String ten);
    boolean existsByTenIgnoreCaseAndIdNot(String ten, Long id);
}
