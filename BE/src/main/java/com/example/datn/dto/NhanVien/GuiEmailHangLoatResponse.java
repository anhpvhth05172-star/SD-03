package com.example.datn.dto.NhanVien;

import java.util.ArrayList;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuiEmailHangLoatResponse {

    private int tongYeuCau;
    private int guiThanhCong;
    private int thatBai;

    @Builder.Default
    private List<BoQua> boQua = new ArrayList<>();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BoQua {
        private Long id;
        private String maNhanVien;
        private String tenTaiKhoan;
        private String email;
        private String lyDo;
    }
}
