package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.example.datn.dto.NhanVien.NhanVienRequest;
import com.example.datn.entity.VaiTro;
import com.example.datn.repository.NhanVienRepository;
import com.example.datn.repository.VaiTroRepository;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * RED/GREEN cho Task 11 plan SD-03: chống trùng số điện thoại nhân viên.
 */
class NhanVienSdtTrungTest {

    private NhanVienRepository repo;
    private NhanVienService service;

    @BeforeEach
    void setUp() {
        repo = mock(NhanVienRepository.class);
        VaiTroRepository vaiTroRepo = mock(VaiTroRepository.class);
        VaiTro staff = new VaiTro();
        staff.setMaVaiTro("STAFF");
        when(vaiTroRepo.findById(1L)).thenReturn(Optional.of(staff));
        service = new NhanVienService(repo, vaiTroRepo, null, null);
    }

    private NhanVienRequest req(String sdt) {
        NhanVienRequest r = new NhanVienRequest();
        r.setTenTaiKhoan("nhan vien test sdt");
        r.setEmail("sdttest@gmail.com");
        r.setSoDienThoai(sdt);
        r.setGioiTinh("Nam");
        r.setNgaySinh(LocalDate.of(1995, 5, 20));
        r.setIdVaiTro(1L);
        return r;
    }

    @Test
    void taoMoiTrungSoDienThoaiBaoLoi() {
        when(repo.existsBySoDienThoai("0912345678")).thenReturn(true);
        assertThatThrownBy(() -> service.kiemTraHopLe(req("0912345678"), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Số điện thoại đã được sử dụng");
    }

    @Test
    void capNhatGiuSoDienThoaiCuaMinhChoPhep() {
        when(repo.existsBySoDienThoaiAndIdNot("0912345678", 5L)).thenReturn(false);
        assertThatCode(() -> service.kiemTraHopLe(req("0912345678"), 5L))
                .doesNotThrowAnyException();
    }

    @Test
    void haiNhanVienSoDienThoaiKhacNhauChoPhep() {
        when(repo.existsBySoDienThoai("0912345678")).thenReturn(false);
        when(repo.existsBySoDienThoai("0987654321")).thenReturn(false);
        assertThatCode(() -> {
            service.kiemTraHopLe(req("0912345678"), null);
            service.kiemTraHopLe(req("0987654321"), null);
        }).doesNotThrowAnyException();
    }
}
