package com.example.datn.service;

import com.example.datn.dto.LichLamViec.ImportLichRequest;
import com.example.datn.dto.LichLamViec.ImportLichResponse;
import com.example.datn.dto.LichLamViec.ImportLichRow;
import com.example.datn.dto.LichLamViec.ImportLoiRow;
import com.example.datn.dto.LichLamViec.LichLamViecDTO;
import com.example.datn.dto.LichLamViec.LichLamViecRequest;
import com.example.datn.dto.LichLamViec.XepTuDongResponse;
import com.example.datn.entity.CaLam;
import com.example.datn.entity.LichLamViec;
import com.example.datn.entity.NhanVien;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.CaLamRepository;
import com.example.datn.repository.LichLamViecRepository;
import com.example.datn.repository.NhanVienRepository;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LichLamViecService {

    private final LichLamViecRepository lichRepo;
    private final NhanVienRepository nvRepo;
    private final CaLamRepository caRepo;

    @Transactional(readOnly = true)
    public List<LichLamViecDTO> list(LocalDate tu, LocalDate den, String tim, Long idCa, LocalDate ngay) {
        return lichRepo.locLich(tu, den, ngay, idCa, tim).stream()
            .map(LichLamViecService::toDTO)
            .toList();
    }

    @Transactional
    public LichLamViecDTO create(LichLamViecRequest req) {
        NhanVien nv = nvRepo.findByMaNhanVien(req.maNhanVien())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên " + req.maNhanVien()));
        CaLam ca = timCaBat(req.idCa());
       kiemTraTrung(null, nv.getId(), req.ngay(), ca.getId());
        LichLamViec lich = new LichLamViec();
        lich.setNhanVien(nv);
        lich.setCaLam(ca);
        lich.setNgayLam(req.ngay());
        LichLamViec saved = lichRepo.save(lich);
        return toDTO(saved);
    }

    @Transactional
    public LichLamViecDTO update(Long id, LichLamViecRequest req) {
        LichLamViec existing = lichRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy lịch làm việc"));
        NhanVien nv = nvRepo.findByMaNhanVien(req.maNhanVien())
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy nhân viên " + req.maNhanVien()));
        CaLam ca = timCaBat(req.idCa());
        kiemTraTrung(id, nv.getId(), req.ngay(), ca.getId());
        existing.setNhanVien(nv);
        existing.setCaLam(ca);
        existing.setNgayLam(req.ngay());
        LichLamViec saved = lichRepo.save(existing);
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        if (!lichRepo.existsById(id)) {
            throw new ResourceNotFoundException("Không tìm thấy lịch làm việc");
        }
        lichRepo.deleteById(id);
    }

    @Transactional
    public void deleteTheoNgay(LocalDate ngay) {
        lichRepo.deleteByNgayLam(ngay);
    }

    @Transactional
    public XepTuDongResponse xepTuDong(LocalDate ngay) {
        List<CaLam> cas = caRepo.findByDangBatTrueOrderByBatDauAsc();
        if (cas.isEmpty()) {
            return new XepTuDongResponse("khongCa", 0);
        }
        List<NhanVien> chuaCoLich = nvRepo.findByTrangThaiTrue().stream()
            .filter(nv -> cas.stream().noneMatch(c ->
                lichRepo.existsByNhanVienIdAndNgayLamAndCaLamId(nv.getId(), ngay, c.getId())))
            .toList();
        if (chuaCoLich.isEmpty()) {
            return new XepTuDongResponse("khongUngVien", 0);
        }
        List<LichLamViec> moi = new ArrayList<>();
        int i = 0;
        for (NhanVien nv : chuaCoLich) {
            CaLam ca = cas.get(i % cas.size());
            LichLamViec l = new LichLamViec();
            l.setNhanVien(nv);
            l.setCaLam(ca);
            l.setNgayLam(ngay);
            moi.add(l);
            i++;
        }
        lichRepo.saveAll(moi);
        return new XepTuDongResponse(null, moi.size());
    }

    @Transactional
    public ImportLichResponse importLich(ImportLichRequest req) {
        List<ImportLoiRow> loi = new ArrayList<>();
        List<LichLamViec> hopLe = new ArrayList<>();
        Set<String> trongBatch = new HashSet<>();

        List<ImportLichRow> rows = req.rows() == null ? List.of() : req.rows();
        for (int i = 0; i < rows.size(); i++) {
            ImportLichRow row = rows.get(i);
            int dong = i + 1;
            NhanVien nv = nvRepo.findByMaNhanVien(row.maNhanVien()).orElse(null);
            if (nv == null) {
                loi.add(new ImportLoiRow(dong, "Nhân viên " + row.maNhanVien() + " không tồn tại"));
                continue;
            }
            CaLam ca = caRepo.findByMaCa(row.maCa()).orElse(null);
            if (ca == null) {
                loi.add(new ImportLoiRow(dong, "Ca " + row.maCa() + " không tồn tại"));
                continue;
            }
            if (!ca.getDangBat()) {
                loi.add(new ImportLoiRow(dong, "Ca \"" + ca.getTenCa() + "\" đang tắt - hãy bật ca trước"));
                continue;
            }
            if (row.ngay() == null) {
                loi.add(new ImportLoiRow(dong, "Ngày không hợp lệ"));
                continue;
            }
            String key = nv.getId() + "|" + row.ngay() + "|" + ca.getId();
            boolean trungDb = lichRepo.existsByNhanVienIdAndNgayLamAndCaLamId(nv.getId(), row.ngay(), ca.getId());
            if (trungDb || !trongBatch.add(key)) {
                loi.add(new ImportLoiRow(dong, "Trống lịch làm việc"));
                continue;
            }
            LichLamViec l = new LichLamViec();
            l.setNhanVien(nv);
            l.setCaLam(ca);
            l.setNgayLam(row.ngay());
            hopLe.add(l);
        }
        if (!hopLe.isEmpty()) {
            lichRepo.saveAll(hopLe);
        }
        return new ImportLichResponse(hopLe.size(), loi);
    }

    private CaLam timCaBat(Long idCa) {
        CaLam ca = caRepo.findById(idCa)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca làm"));
        if (!ca.getDangBat()) {
            throw new IllegalArgumentException("Ca \"" + ca.getTenCa() + "\" đang tắt - hãy bật ca trước");
        }
        return ca;
    }

    private void kiemTraTrung(Long idHienTai, Long idNv, LocalDate ngay, Long idCa) {
        boolean trung = idHienTai == null
            ? lichRepo.existsByNhanVienIdAndNgayLamAndCaLamId(idNv, ngay, idCa)
            : lichRepo.existsByNhanVienIdAndNgayLamAndCaLamIdAndIdNot(idNv, ngay, idCa, idHienTai);
        if (trung) {
            throw new DuplicateRecordException("Trống lịch làm việc");
        }
    }

    static LichLamViecDTO toDTO(LichLamViec l) {
        NhanVien nv = l.getNhanVien();
        CaLam ca = l.getCaLam();
        return new LichLamViecDTO(
            l.getId(),
            nv.getMaNhanVien(),
            nv.getTenTaiKhoan(),
            ca.getId(),
            ca.getTenCa(),
            ca.getBatDau(),
            ca.getKetThuc(),
            l.getNgayLam()
        );
    }
}
