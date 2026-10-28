package com.example.datn.repository;

import com.example.datn.dto.ThongKe.ChiTietThongKeRow;
import com.example.datn.dto.ThongKe.HoaDonThongKeRow;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ThongKeRepository extends JpaRepository<com.example.datn.entity.HoaDon, Long> {

    @Query("""
        SELECT new com.example.datn.dto.ThongKe.HoaDonThongKeRow(
            h.id, h.trangThai, h.tienSauGiamGia, h.daXoa,
            ptt.hinhThucThanhToan.maHinhThuc)
        FROM HoaDon h
        LEFT JOIN h.phuongThucThanhToan ptt
        LEFT JOIN ptt.hinhThucThanhToan
        WHERE h.ngayTao >= :tu AND h.ngayTao < :den
        """)
    List<HoaDonThongKeRow> findHoaDonTrongKhoang(
        @Param("tu") LocalDateTime tu,
        @Param("den") LocalDateTime den
    );

    @Query("""
        SELECT new com.example.datn.dto.ThongKe.ChiTietThongKeRow(
            c.hoaDon.id, c.hoaDon.trangThai, c.hoaDon.daXoa,
            th.tenThuongHieu, c.thanhTien, c.soLuong)
        FROM ChiTietHoaDon c
        JOIN c.hoaDon h
        JOIN c.sanPhamChiTiet spct
        JOIN spct.sanPham sp
        LEFT JOIN sp.thuongHieu th
        WHERE h.ngayTao >= :tu AND h.ngayTao < :den
        """)
    List<ChiTietThongKeRow> findChiTietTrongKhoang(
        @Param("tu") LocalDateTime tu,
        @Param("den") LocalDateTime den
    );

    @Query("SELECT COUNT(k) FROM KhachHang k WHERE k.ngayTao >= :tu AND k.ngayTao < :den")
    long countKhachMoi(@Param("tu") LocalDateTime tu, @Param("den") LocalDateTime den);

    @Query("SELECT DISTINCT YEAR(h.ngayTao) FROM HoaDon h ORDER BY YEAR(h.ngayTao) DESC")
    List<Integer> cacNamCoDuLieu();
}
