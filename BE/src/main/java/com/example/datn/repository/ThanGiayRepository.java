package com.example.datn.repository;

import com.example.datn.entity.ThanGiay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ThanGiayRepository extends JpaRepository<ThanGiay, Long> {

    boolean existsByTenThanGiayIgnoreCase(String ten);

    boolean existsByTenThanGiayIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaThanGiayIgnoreCase(String ma);

    boolean existsByMaThanGiayIgnoreCaseAndIdNot(String ma, Long id);

    List<ThanGiay> findByTenThanGiayContainingIgnoreCase(String ten);
}
