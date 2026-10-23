package com.example.datn.repository;

import com.example.datn.entity.CaLam;
import java.time.LocalTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CaLamRepository extends JpaRepository<CaLam, Long> {

    boolean existsByMaCa(String maCa);

    boolean existsByTenCaIgnoreCase(String tenCa);

    boolean existsByTenCaIgnoreCaseAndIdNot(String tenCa, Long id);

    List<CaLam> findByDangBatTrueOrderByBatDauAsc();

    @Query("SELECT COUNT(l) FROM LichLamViec l WHERE l.caLam.id = :idCa")
    long demLichTheoCa(@Param("idCa") Long idCa);

    @Query("""
        SELECT c FROM CaLam c
        WHERE c.id != :id
          AND c.batDau < :ketThuc
          AND c.ketThuc > :batDau
        """)
    List<CaLam> timCaTrungKhungGio(
        @Param("id") Long id,
        @Param("batDau") LocalTime batDau,
        @Param("ketThuc") LocalTime ketThuc
    );
}
