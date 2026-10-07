package com.example.datn.repository;

import com.example.datn.entity.SanPham;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface SanPhamRepository extends JpaRepository<SanPham, Long> {

    boolean existsByTenSanPhamIgnoreCase(String tenSanPham);

    boolean existsByTenSanPhamIgnoreCaseAndIdNot(String tenSanPham, Long id);

    boolean existsByMaSanPhamIgnoreCase(String maSanPham);

    Optional<SanPham> findFirstByOrderByIdDesc();

    @Query(value = "SELECT sp FROM SanPham sp " +
           "LEFT JOIN sp.xuatXu xx " +
           "LEFT JOIN sp.thuongHieu th " +
           "LEFT JOIN sp.chatLieu cl " +
           "LEFT JOIN sp.kieuDang kd " +
           "LEFT JOIN sp.loaiGiay lg " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR sp.tenSanPham LIKE CONCAT('%', :keyword, '%') OR sp.maSanPham LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:idThuongHieu IS NULL OR th.id = :idThuongHieu) " +
           "AND (:idLoaiGiay IS NULL OR lg.id = :idLoaiGiay) " +
           "AND (:idChatLieu IS NULL OR cl.id = :idChatLieu) " +
           "AND (:idKieuDang IS NULL OR kd.id = :idKieuDang) " +
           "AND (:idXuatXu IS NULL OR xx.id = :idXuatXu) " +
           "AND (:doiTuong IS NULL OR :doiTuong = '' OR sp.doiTuong = :doiTuong) " +
           "AND (:trangThai IS NULL OR sp.trangThai = :trangThai)",
           countQuery = "SELECT COUNT(sp.id) FROM SanPham sp " +
           "WHERE (:keyword IS NULL OR :keyword = '' OR sp.tenSanPham LIKE CONCAT('%', :keyword, '%') OR sp.maSanPham LIKE CONCAT('%', :keyword, '%')) " +
           "AND (:idThuongHieu IS NULL OR sp.thuongHieu.id = :idThuongHieu) " +
           "AND (:idLoaiGiay IS NULL OR sp.loaiGiay.id = :idLoaiGiay) " +
           "AND (:idChatLieu IS NULL OR sp.chatLieu.id = :idChatLieu) " +
           "AND (:idKieuDang IS NULL OR sp.kieuDang.id = :idKieuDang) " +
           "AND (:idXuatXu IS NULL OR sp.xuatXu.id = :idXuatXu) " +
           "AND (:doiTuong IS NULL OR :doiTuong = '' OR sp.doiTuong = :doiTuong) " +
           "AND (:trangThai IS NULL OR sp.trangThai = :trangThai)")
    Page<SanPham> filterSanPhams(
            @Param("keyword") String keyword,
            @Param("idThuongHieu") Long idThuongHieu,
            @Param("idLoaiGiay") Long idLoaiGiay,
            @Param("idChatLieu") Long idChatLieu,
            @Param("idKieuDang") Long idKieuDang,
            @Param("idXuatXu") Long idXuatXu,
            @Param("doiTuong") String doiTuong,
            @Param("trangThai") Boolean trangThai,
            Pageable pageable
    );
}
