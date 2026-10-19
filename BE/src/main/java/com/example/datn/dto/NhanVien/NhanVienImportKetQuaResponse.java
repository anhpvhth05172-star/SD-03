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
public class NhanVienImportKetQuaResponse {

    private int themThanhCong;
    private int capNhatThanhCong;
    private int thatBai;

    @Builder.Default
    private List<String> chiTietLoi = new ArrayList<>();
}
