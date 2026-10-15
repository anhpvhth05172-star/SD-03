package com.example.datn.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(
    name = "hoi_thoai",
    indexes = {
        @Index(name = "IX_hoi_thoai_ma_an_danh", columnList = "ma_an_danh"),
        @Index(name = "IX_hoi_thoai_khach_hang", columnList = "id_khach_hang"),
        @Index(name = "IX_hoi_thoai_loai_trang_thai", columnList = "loai,trang_thai")
    }
)
public class HoiThoai {

    public static final String LOAI_NHAN_VIEN = "NHAN_VIEN";
    public static final String LOAI_AI = "AI";
    public static final String TRANG_THAI_DANG_MO = "DANG_MO";
    public static final String TRANG_THAI_DONG = "DONG";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "loai", nullable = false, length = 20)
    private String loai = LOAI_NHAN_VIEN;

    @Column(name = "trang_thai", nullable = false, length = 20)
    private String trangThai = TRANG_THAI_DANG_MO;

    @ManyToOne
    @JoinColumn(name = "id_khach_hang")
    private KhachHang khachHang;

    @Column(name = "ma_an_danh", length = 64)
    private String maAnDanh;

    @ManyToOne
    @JoinColumn(name = "id_nhan_vien_phu_trach")
    private NhanVien nhanVienPhuTrach;

    @Column(name = "ten_khach", length = 100)
    private String tenKhach;

    @Column(name = "tong_tin_khach", nullable = false)
    private int tongTinKhach;

    @Column(name = "tong_tin_nhan_vien", nullable = false)
    private int tongTinNhanVien;

    @Column(name = "ngay_tao", insertable = false, updatable = false)
    private LocalDateTime ngayTao;

    @Column(name = "ngay_cap_nhat")
    private LocalDateTime ngayCapNhat;

    @OneToMany(mappedBy = "hoiThoai")
    @OrderBy("id ASC")
    private List<TinNhan> tinNhanList = new ArrayList<>();
}
