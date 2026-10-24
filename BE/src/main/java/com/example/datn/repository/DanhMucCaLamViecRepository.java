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

    @Query("SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END FROM DanhMucCaLamViec c "
        + "WHERE c.batDau < :ketThuc AND c.ketThuc > :batDau AND (:boQuaId IS NULL OR c.id <> :boQuaId)")
    boolean existsCaBiChongGio(@Param("batDau") LocalTime batDau, @Param("ketThuc") LocalTime ketThuc,
        @Param("boQuaId") Long boQuaId);
}
