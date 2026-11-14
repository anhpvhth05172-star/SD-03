package com.example.datn.repository;

import com.example.datn.dto.ThongKe.ChiTietThongKeRow;
import com.example.datn.dto.ThongKe.HoaDonChiTietThongKeRow;
import com.example.datn.dto.ThongKe.HoaDonThongKeRow;
import com.example.datn.dto.ThongKe.SanPhamDaBanChiTietRow;
import com.example.datn.dto.ThongKe.SanPhamThongKeRow;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ThongKeRepository extends JpaRepository<com.example.datn.entity.HoaDon, Long> {

    @Query("""
        SELECT new com.example.datn.dto.ThongKe.HoaDonThongKeRow(
            h.id, h.trangThai, h.tienSauGiamGia, h.daXoa,
            ptt.hinhThucThanhToan.maHinhThuc, h.loaiDon)
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

    @Query("""
        SELECT new com.example.datn.dto.ThongKe.SanPhamThongKeRow(
            h.id, h.trangThai, h.daXoa, h.ngayTao,
            sp.maSanPham, sp.tenSanPham, th.tenThuongHieu, c.thanhTien, c.soLuong)
        FROM ChiTietHoaDon c
        JOIN c.hoaDon h
        JOIN c.sanPhamChiTiet spct
        JOIN spct.sanPham sp
        LEFT JOIN sp.thuongHieu th
        WHERE h.ngayTao >= :tu AND h.ngayTao < :den
        """)
    List<SanPhamThongKeRow> findSanPhamTrongKhoang(
        @Param("tu") LocalDateTime tu,
        @Param("den") LocalDateTime den
    );

    @Query("""
        SELECT new com.example.datn.dto.ThongKe.HoaDonChiTietThongKeRow(
            h.id, h.maHoaDon, h.ngayTao, h.tenKhachHang, h.soDienThoaiKhachHang,
            nv.tenTaiKhoan, h.loaiDon,
            COALESCE(htt.tenHinhThuc, ptt.tenPhuongThuc, 'Chưa xác định'),
            htt.maHinhThuc,
            h.trangThai, h.tienSauGiamGia, h.daXoa)
        FROM HoaDon h
        LEFT JOIN h.nhanVien nv
        LEFT JOIN h.phuongThucThanhToan ptt
        LEFT JOIN ptt.hinhThucThanhToan htt
        WHERE h.ngayTao >= :tu AND h.ngayTao < :den
        ORDER BY h.ngayTao DESC
        """)
    List<HoaDonChiTietThongKeRow> findHoaDonChiTietTrongKhoang(
        @Param("tu") LocalDateTime tu,
        @Param("den") LocalDateTime den
    );

    @Query("""
        SELECT new com.example.datn.dto.ThongKe.SanPhamDaBanChiTietRow(
            h.id, h.maHoaDon, h.ngayTao,
            sp.maSanPham, sp.tenSanPham,
            kc.tenKichCo, ms.tenMau,
            th.tenThuongHieu,
            c.soLuong, c.donGia, c.thanhTien,
            h.trangThai, h.daXoa)
        FROM ChiTietHoaDon c
        JOIN c.hoaDon h
        JOIN c.sanPhamChiTiet spct
        JOIN spct.sanPham sp
        LEFT JOIN spct.kichCo kc
        LEFT JOIN spct.mauSac ms
        LEFT JOIN sp.thuongHieu th
        WHERE h.ngayTao >= :tu AND h.ngayTao < :den
        ORDER BY h.ngayTao DESC
        """)
    List<SanPhamDaBanChiTietRow> findSanPhamDaBanChiTietTrongKhoang(
        @Param("tu") LocalDateTime tu,
        @Param("den") LocalDateTime den
    );
}
