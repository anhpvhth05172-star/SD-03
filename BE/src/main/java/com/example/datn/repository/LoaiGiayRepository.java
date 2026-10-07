package com.example.datn.repository;

import com.example.datn.entity.LoaiGiay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoaiGiayRepository extends JpaRepository<LoaiGiay, Long> {

    boolean existsByTenLoaiGiayIgnoreCase(String ten);

    boolean existsByTenLoaiGiayIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaLoaiGiayIgnoreCase(String ma);

    boolean existsByMaLoaiGiayIgnoreCaseAndIdNot(String ma, Long id);

    List<LoaiGiay> findByTenLoaiGiayContainingIgnoreCaseOrMaLoaiGiayContainingIgnoreCase(String ten, String ma);
}
