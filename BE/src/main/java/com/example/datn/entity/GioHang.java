    package com.example.datn.entity;

    import jakarta.persistence.*;
    import lombok.AllArgsConstructor;
    import lombok.Getter;
    import lombok.NoArgsConstructor;
    import lombok.Setter;

    import java.time.LocalDateTime;
    @Setter
    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    @Entity
    public class GioHang {

        @Id
        @GeneratedValue(strategy = GenerationType.IDENTITY)
        private Integer id;

        private String maGioHang;

        private LocalDateTime ngayTao;

        private LocalDateTime ngayCapNhat;

        private String trangThai;

        @ManyToOne
        @JoinColumn(name = "id_khach_hang", referencedColumnName = "id")
        private KhachHang khachHang;
    }
