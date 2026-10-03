package com.example.datn.repository;

import com.example.datn.entity.ThuongHieu;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThuongHieuRepository extends JpaRepository<ThuongHieu, Long> {

    boolean existsByTenThuongHieuIgnoreCase(String ten);

    boolean existsByTenThuongHieuIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaThuongHieuIgnoreCase(String ma);

    boolean existsByMaThuongHieuIgnoreCaseAndIdNot(String ma, Long id);

    Optional<ThuongHieu> findByMaThuongHieu(String ma);

    List<ThuongHieu> findByTenThuongHieuContainingIgnoreCaseOrMaThuongHieuContainingIgnoreCase(String ten, String ma);
}
