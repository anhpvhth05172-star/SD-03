package com.example.datn.repository;

import com.example.datn.entity.LichLamViec;
import java.time.LocalDate;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LichLamViecRepository extends JpaRepository<LichLamViec, Long> {
    @Query("""
        SELECT l FROM LichLamViec l
        JOIN FETCH l.caLam c
        WHERE l.nhanVien.id = :idNhanVien
          AND l.ngayLam = :ngayLam
          AND c.dangBat = true
        ORDER BY c.batDau ASC
        """)
    List<LichLamViec> findCaDangBatDuocXep(
        @Param("idNhanVien") Long idNhanVien,
        @Param("ngayLam") LocalDate ngayLam
    );


    boolean existsByNhanVienIdAndNgayLamAndCaLamId(
        @Param("idNhanVien") Long idNhanVien,
        @Param("ngayLam") LocalDate ngayLam,
        @Param("idCaLam") Long idCaLam
    );

    boolean existsByNhanVienIdAndNgayLamAndCaLamIdAndIdNot(
        @Param("idNhanVien") Long idNhanVien,
        @Param("ngayLam") LocalDate ngayLam,
        @Param("idCaLam") Long idCaLam,
        @Param("id") Long id
    );

    @Query("""
        SELECT l FROM LichLamViec l
        JOIN FETCH l.nhanVien n
        JOIN FETCH l.caLam c
        WHERE (:tu IS NULL OR l.ngayLam >= :tu)
          AND (:den IS NULL OR l.ngayLam <= :den)
          AND (:ngay IS NULL OR l.ngayLam = :ngay)
          AND (:idCa IS NULL OR c.id = :idCa)
          AND (:tim IS NULL
               OR LOWER(n.maNhanVien) LIKE LOWER(CONCAT('%', :tim, '%'))
               OR LOWER(n.tenTaiKhoan) LIKE LOWER(CONCAT('%', :tim, '%')))
        ORDER BY l.ngayLam DESC, c.batDau ASC
        """)
    List<LichLamViec> locLich(
        @Param("tu") LocalDate tu,
        @Param("den") LocalDate den,
        @Param("ngay") LocalDate ngay,
        @Param("idCa") Long idCa,
        @Param("tim") String tim
    );

    @Modifying
    @Query("DELETE FROM LichLamViec l WHERE l.ngayLam = :ngay")
    void deleteByNgayLam(@Param("ngay") LocalDate ngay);

    @Query("""
        SELECT l FROM LichLamViec l
        JOIN FETCH l.nhanVien n
        JOIN FETCH l.caLam c
        WHERE l.ngayLam >= :tu AND l.ngayLam <= :den
        """)
    List<LichLamViec> findByNgayLamBetween(
        @Param("tu") LocalDate tu,
        @Param("den") LocalDate den
    );

    @Modifying
    @Query("DELETE FROM LichLamViec l WHERE l.ngayLam >= :tu AND l.ngayLam <= :den")
    void deleteByNgayLamBetween(@Param("tu") LocalDate tu, @Param("den") LocalDate den);

    @Modifying
    @Query("DELETE FROM LichLamViec l WHERE l.ngayLam >= :tu AND l.ngayLam <= :den AND l.caLam.id IN :caIds")
    void deleteByNgayLamBetweenAndCaLamIdIn(
        @Param("tu") LocalDate tu,
        @Param("den") LocalDate den,
        @Param("caIds") List<Long> caIds
    );
}
