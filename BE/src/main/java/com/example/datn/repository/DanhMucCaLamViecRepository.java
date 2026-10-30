package com.example.datn.repository;

import com.example.datn.entity.DanhMucCaLamViec;
import java.util.List;
import java.time.LocalTime;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DanhMucCaLamViecRepository extends JpaRepository<DanhMucCaLamViec, Long> {
    List<DanhMucCaLamViec> findAllByOrderByTenAsc();
    boolean existsByMa(String ma);
    boolean existsByMaAndIdNot(String ma, Long id);
    boolean existsByTenIgnoreCase(String ten);
    boolean existsByTenIgnoreCaseAndIdNot(String ten, Long id);

    @Query(value = "SELECT CAST(CASE WHEN EXISTS (SELECT 1 FROM dbo.danh_muc_ca_lam_viec "
        + "WHERE gio_bat_dau < CONVERT(time(0), :ketThuc) "
        + "AND gio_ket_thuc > CONVERT(time(0), :batDau) "
        + "AND (:boQuaId IS NULL OR id <> :boQuaId)) THEN 1 ELSE 0 END AS bit)", nativeQuery = true)
    boolean existsCaBiChongGio(@Param("batDau") LocalTime batDau, @Param("ketThuc") LocalTime ketThuc,
        @Param("boQuaId") Long boQuaId);
}
