package com.example.datn.service;

import com.example.datn.dto.LichLamViec.CaLamViecLichDto;
import com.example.datn.dto.LichLamViec.CaLamViecLichRequest;
import com.example.datn.dto.LichLamViec.LichLamViecDto;
import com.example.datn.dto.LichLamViec.LichLamViecRequest;
import com.example.datn.entity.DanhMucCaLamViec;
import com.example.datn.entity.LichLamViec;
import com.example.datn.entity.NhanVien;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.DanhMucCaLamViecRepository;
import com.example.datn.repository.LichLamViecRepository;
import com.example.datn.repository.NhanVienRepository;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class LichLamViecService {

    private final DanhMucCaLamViecRepository caRepository;
    private final LichLamViecRepository lichRepository;
    private final NhanVienRepository nhanVienRepository;

    @Transactional(readOnly = true)
    public List<CaLamViecLichDto> danhSachCa() {
        return caRepository.findAllByOrderByTenAsc().stream().map(this::toDto).toList();
    }

    public CaLamViecLichDto themCa(CaLamViecLichRequest request) {
        kiemTraCa(request, null);
        DanhMucCaLamViec ca = new DanhMucCaLamViec();
        capNhat(ca, request);
        return toDto(caRepository.save(ca));
    }

    public CaLamViecLichDto capNhatCa(Long id, CaLamViecLichRequest request) {
        DanhMucCaLamViec ca = timCa(id);
        kiemTraCa(request, id);
        capNhat(ca, request);
        return toDto(caRepository.save(ca));
    }

    public CaLamViecLichDto doiTrangThaiCa(Long id) {
        DanhMucCaLamViec ca = timCa(id);
        ca.setDangBat(!ca.isDangBat());
        return toDto(caRepository.save(ca));
    }

    public void xoaCa(Long id) {
        DanhMucCaLamViec ca = timCa(id);
        if (lichRepository.existsByCaLamViecId(id)) {
            throw new IllegalArgumentException("Ca làm đang được dùng trong lịch, hãy xóa lịch đó trước");
        }
        caRepository.delete(ca);
    }

    @Transactional(readOnly = true)
    public List<LichLamViecDto> danhSachLich(LocalDate tuNgay, LocalDate denNgay) {
        return lichRepository.findAllByOrderByNgayAscIdAsc().stream()
            .filter(row -> tuNgay == null || !row.getNgay().isBefore(tuNgay))
            .filter(row -> denNgay == null || !row.getNgay().isAfter(denNgay))
            .map(this::toDto)
            .toList();
    }

    public LichLamViecDto themLich(LichLamViecRequest request) {
        return luuLich(null, request);
    }

    public LichLamViecDto capNhatLich(Long id, LichLamViecRequest request) {
        LichLamViec lich = lichRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch làm việc"));
        return luuLich(lich, request);
    }

    public void xoaLich(Long id) {
        if (!lichRepository.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy lịch làm việc");
        }
        lichRepository.deleteById(id);
    }

    public void xoaLichTheoNgay(LocalDate ngay) {
        lichRepository.deleteAllByNgay(ngay);
    }

    private LichLamViecDto luuLich(LichLamViec lich, LichLamViecRequest request) {
        if (request.ngay() == null || request.maNhanVien() == null || request.maNhanVien().isBlank() || request.idCa() == null) {
            throw new IllegalArgumentException("Vui lòng chọn nhân viên, ca làm và ngày làm");
        }
        NhanVien nhanVien = nhanVienRepository.findByMaNhanVien(request.maNhanVien().trim())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên " + request.maNhanVien()));
        if (!Boolean.TRUE.equals(nhanVien.getTrangThai())) {
            throw new IllegalArgumentException("Không thể xếp lịch cho nhân viên đã ngừng hoạt động");
        }
        DanhMucCaLamViec ca = timCa(request.idCa());
        if (!ca.isDangBat()) {
            throw new IllegalArgumentException("Không thể xếp lịch vào ca đã tắt");
        }
        Long idHienTai = lich == null ? null : lich.getId();
        boolean biTrung = idHienTai == null
            ? lichRepository.existsByNhanVienIdAndCaLamViecIdAndNgay(
                nhanVien.getId(), ca.getId(), request.ngay())
            : lichRepository.existsByNhanVienIdAndCaLamViecIdAndNgayAndIdNot(
                nhanVien.getId(), ca.getId(), request.ngay(), idHienTai);
        if (biTrung) {
            throw new DuplicateRecordException("Nhân viên đã có lịch ca này trong ngày");
        }
        if (lich == null) lich = new LichLamViec();
        lich.setNhanVien(nhanVien);
        lich.setCaLamViec(ca);
        lich.setNgay(request.ngay());
        return toDto(lichRepository.save(lich));
    }

    private void kiemTraCa(CaLamViecLichRequest request, Long id) {
        if (request.batDau() == null || request.ketThuc() == null || !request.batDau().isBefore(request.ketThuc())) {
            throw new IllegalArgumentException("Giờ kết thúc phải sau giờ bắt đầu");
        }
        String ma = request.ma().trim().toLowerCase();
        String ten = request.ten().trim();
        boolean maTrung = id == null ? caRepository.existsByMa(ma) : caRepository.existsByMaAndIdNot(ma, id);
        boolean tenTrung = id == null ? caRepository.existsByTenIgnoreCase(ten) : caRepository.existsByTenIgnoreCaseAndIdNot(ten, id);
        if (maTrung) throw new DuplicateRecordException("Mã ca làm đã tồn tại");
        if (tenTrung) throw new DuplicateRecordException("Tên ca làm đã tồn tại");
        if (caRepository.existsCaBiChongGio(request.batDau(), request.ketThuc(), id)) {
            throw new DuplicateRecordException("Khung giờ này trùng với ca làm khác");
        }
    }

    private void capNhat(DanhMucCaLamViec ca, CaLamViecLichRequest request) {
        ca.setMa(request.ma().trim().toLowerCase());
        ca.setTen(request.ten().trim());
        ca.setBatDau(request.batDau());
        ca.setKetThuc(request.ketThuc());
    }

    private DanhMucCaLamViec timCa(Long id) {
        return caRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca làm"));
    }

    private CaLamViecLichDto toDto(DanhMucCaLamViec ca) {
        return new CaLamViecLichDto(ca.getId(), ca.getMa(), ca.getTen(), ca.getBatDau(), ca.getKetThuc(), ca.isDangBat());
    }

    private LichLamViecDto toDto(LichLamViec lich) {
        return new LichLamViecDto(
            lich.getId(),
            lich.getNhanVien().getMaNhanVien(),
            lich.getNhanVien().getTenTaiKhoan(),
            lich.getCaLamViec().getId(),
            lich.getNgay()
        );
    }
}
