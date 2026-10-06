package com.example.datn.repository;

import com.example.datn.entity.DotGiamGia;
import org.hibernate.query.Page;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DotGiamGiaRepository extends JpaRepository<DotGiamGia, Long> {
}
