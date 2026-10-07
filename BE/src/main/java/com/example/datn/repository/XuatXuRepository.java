package com.example.datn.repository;

import com.example.datn.entity.XuatXu;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface XuatXuRepository extends JpaRepository<XuatXu, Long> {

    boolean existsByTenXuatXuIgnoreCase(String ten);

    boolean existsByTenXuatXuIgnoreCaseAndIdNot(String ten, Long id);

    boolean existsByMaXuatXuIgnoreCase(String ma);

    boolean existsByMaXuatXuIgnoreCaseAndIdNot(String ma, Long id);

    List<XuatXu> findByTenXuatXuContainingIgnoreCaseOrMaXuatXuContainingIgnoreCase(String ten, String ma);
}
