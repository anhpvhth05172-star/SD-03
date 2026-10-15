package com.example.datn.repository;

import com.example.datn.entity.HoiThoai;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface HoiThoaiRepository extends JpaRepository<HoiThoai, Long> {

    Optional<HoiThoai> findFirstByLoaiAndMaAnDanhAndTrangThaiOrderByNgayCapNhatDesc(
        String loai,
        String maAnDanh,
        String trangThai
    );

    Optional<HoiThoai> findFirstByLoaiAndKhachHangIdAndTrangThaiOrderByNgayCapNhatDesc(
        String loai,
        Long idKhachHang,
        String trangThai
    );

    List<HoiThoai> findByLoaiOrderByNgayCapNhatDesc(String loai);

    @Query(
        """
        SELECT h FROM HoiThoai h
        WHERE h.loai = :loai
          AND (h.nhanVienPhuTrach IS NULL OR h.nhanVienPhuTrach.id = :idNhanVien)
        ORDER BY h.ngayCapNhat DESC
        """
    )
    List<HoiThoai> timHoiThoaiChoNhanVien(@Param("loai") String loai, @Param("idNhanVien") Long idNhanVien);

    Optional<HoiThoai> findByIdAndLoai(Long id, String loai);

    boolean existsByMaAnDanh(String maAnDanh);
}
