package com.example.datn.repository;

import com.example.datn.entity.MauSac;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MauSacRepository extends JpaRepository<MauSac, Long> {

    boolean existsByTenMauIgnoreCase(String ten);

    boolean existsByTenMauIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaMauIgnoreCase(String ma);

    boolean existsByMaMauIgnoreCaseAndIdNot(String ma, Long id);

    List<MauSac> findByTenMauContainingIgnoreCaseOrMaMauContainingIgnoreCase(String ten, String ma);
}
