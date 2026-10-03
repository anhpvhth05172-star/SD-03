package com.example.datn.repository;

import com.example.datn.entity.KieuDang;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KieuDangRepository extends JpaRepository<KieuDang, Long> {

    boolean existsByTenKieuDangIgnoreCase(String ten);

    boolean existsByTenKieuDangIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaKieuDangIgnoreCase(String ma);

    boolean existsByMaKieuDangIgnoreCaseAndIdNot(String ma, Long id);

    List<KieuDang> findByTenKieuDangContainingIgnoreCaseOrMaKieuDangContainingIgnoreCase(String ten, String ma);
}
