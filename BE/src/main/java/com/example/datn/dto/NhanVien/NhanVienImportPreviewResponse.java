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
public class NhanVienImportPreviewResponse {

    private int tongSoDong;
    private int dongHopLe;
    private int dongLoi;
    private int themMoiDuKien;
    private int capNhatDuKien;

    @Builder.Default
    private List<NhanVienImportDong> dong = new ArrayList<>();

    @Builder.Default
    private List<String> loi = new ArrayList<>();
}
