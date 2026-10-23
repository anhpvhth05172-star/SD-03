package com.example.datn.repository;

import com.example.datn.entity.LichSuCa;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichSuCaRepository extends JpaRepository<LichSuCa, Long> {

    @Query("SELECT l FROM LichSuCa l WHERE l.caLamViec.id = :idCa ORDER BY l.thoiGian DESC, l.id DESC")
    List<LichSuCa> findByCaLamViecId(@Param("idCa") Long idCa);
}
