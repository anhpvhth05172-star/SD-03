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
public class NhanVienExcelImportResult {
    private int tongSoDong;
    private int soDongThanhCong;
    private int soDongThatBai;

    @Builder.Default
    private List<String> danhSachLoi = new ArrayList<>();
}
