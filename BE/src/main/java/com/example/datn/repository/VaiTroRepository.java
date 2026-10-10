package com.example.datn.repository;

import com.example.datn.entity.VaiTro;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VaiTroRepository extends JpaRepository<VaiTro, Long> {

    List<VaiTro> findByTrangThaiTrueOrderByTenVaiTroAsc();

    List<VaiTro> findByMaVaiTroIn(List<String> maVaiTros);

    VaiTro findByMaVaiTro(String maVaiTro);
}