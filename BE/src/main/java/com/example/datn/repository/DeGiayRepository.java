package com.example.datn.repository;

import com.example.datn.entity.DeGiay;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DeGiayRepository extends JpaRepository<DeGiay, Long> {

    boolean existsByTenDeGiayIgnoreCase(String ten);

    boolean existsByTenDeGiayIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaDeGiayIgnoreCase(String ma);

    boolean existsByMaDeGiayIgnoreCaseAndIdNot(String ma, Long id);

    List<DeGiay> findByTenDeGiayContainingIgnoreCase(String ten);
}
