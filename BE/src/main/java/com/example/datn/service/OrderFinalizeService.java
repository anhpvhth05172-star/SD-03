//package com.example.datn.service;
//
//
//import com.example.datn.entity.ChiTietHoaDon;
//import com.example.datn.entity.HoaDon;
//import com.example.datn.entity.SanPhamChiTiet;
//import com.example.datn.repository.ChiTietHoaDonRepository;
//import com.example.datn.repository.GioHangChiTietRepository;
//import com.example.datn.repository.SanPhamChiTietRepository;
//import java.util.List;
//import lombok.RequiredArgsConstructor;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.stereotype.Service;
//import org.springframework.transaction.annotation.Transactional;
//
///**
// * Chốt đơn sau khi thanh toán VNPay thành công:
// * trừ tồn kho và xóa đúng những dòng giỏ hàng đã được đặt.
// *
// * HoaDon KHÔNG có collection chi tiết nên phải lấy qua HoaDonChiTietRepository.
// * Giả định HoaDonChiTiet có: hoaDon, sanPhamChiTiet, soLuong (đổi nếu tên khác).
// */
//@Slf4j
//@Service
//@RequiredArgsConstructor
//public class OrderFinalizeService {
//
//    private final ChiTietHoaDonRepository hoaDonChiTietRepository;
//    private final GioHangChiTietRepository gioHangChiTietRepository;
//    private final SanPhamChiTietRepository sanPhamChiTietRepository;
//
//    @Transactional
//    public void clearCartAndDeductStock(HoaDon hoaDon) {
//        List<ChiTietHoaDon> chiTiets = hoaDonChiTietRepository.findByHoaDon_Id(hoaDon.getId());
//        Long khachHangId = hoaDon.getKhachHang() != null ? hoaDon.getKhachHang().getId() : null;
//
//        for (ChiTietHoaDon ct : chiTiets) {
//            SanPhamChiTiet spct = ct.getSanPhamChiTiet();
//
//            if (spct.getSoLuong() < ct.getSoLuong()) {
//                throw new IllegalStateException(
//                        "Sản phẩm #" + spct.getId() + " không đủ tồn kho");
//            }
//            spct.setSoLuong(spct.getSoLuong() - ct.getSoLuong());
//            sanPhamChiTietRepository.save(spct);
//
//            // Chỉ xóa dòng giỏ hàng tương ứng sản phẩm đã đặt (đơn không có khách thì bỏ qua)
//            if (khachHangId != null) {
//                gioHangChiTietRepository
//                        .deleteByGioHang_KhachHang_IdAndSanPhamChiTiet_Id(khachHangId, spct.getId());
//            }
//        }
//        log.info("Đã xóa giỏ và trừ kho cho đơn {}", hoaDon.getMaHoaDon());
//    }
//}
//
