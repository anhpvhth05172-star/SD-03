package com.example.datn.repository;

import com.example.datn.entity.KichCo;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface KichCoRepository extends JpaRepository<KichCo, Long> {

    boolean existsByTenKichCoIgnoreCase(String ten);

    boolean existsByTenKichCoIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaKichCoIgnoreCase(String ma);

    boolean existsByMaKichCoIgnoreCaseAndIdNot(String ma, Long id);

    List<KichCo> findByTenKichCoContainingIgnoreCaseOrMaKichCoContainingIgnoreCase(String ten, String ma);
}
