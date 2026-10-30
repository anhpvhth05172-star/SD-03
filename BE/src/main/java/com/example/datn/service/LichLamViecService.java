package com.example.datn.service;

import com.example.datn.dto.LichLamViec.ImportLichRequest;
import com.example.datn.dto.LichLamViec.ImportLichResponse;
import com.example.datn.dto.LichLamViec.ImportLichRow;
import com.example.datn.dto.LichLamViec.ImportLoiRow;
import com.example.datn.dto.LichLamViec.LichLamViecDTO;
import com.example.datn.dto.LichLamViec.LichLamViecRequest;
import com.example.datn.dto.LichLamViec.XepTuDongRequest;
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
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
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
        return xepTuDong(new XepTuDongRequest(ngay));
    }

    @Transactional
    public XepTuDongResponse xepTuDong(XepTuDongRequest req) {
        LocalDate tuNgay = req.tuNgay() != null ? req.tuNgay() : (req.ngay() != null ? req.ngay() : LocalDate.now());
        LocalDate denNgay = req.denNgay() != null ? req.denNgay() : (req.ngay() != null ? req.ngay() : tuNgay);

        if (tuNgay.isAfter(denNgay)) {
            throw new IllegalArgumentException("Từ ngày không được sau đến ngày");
        }
        if (tuNgay.plusDays(62).isBefore(denNgay)) {
            throw new IllegalArgumentException("Khoảng thời gian xếp lịch tối đa là 60 ngày");
        }

        // 1. Lấy danh sách ca làm việc
        List<CaLam> cas;
        if (req.idCaLams() != null && !req.idCaLams().isEmpty()) {
            cas = caRepo.findAllById(req.idCaLams()).stream()
                .filter(c -> Boolean.TRUE.equals(c.getDangBat()))
                .sorted(Comparator.comparing(CaLam::getBatDau))
                .toList();
        } else {
            cas = caRepo.findByDangBatTrueOrderByBatDauAsc();
        }
        if (cas.isEmpty()) {
            return new XepTuDongResponse("khongCa", 0, 0, Map.of(), "Không có ca làm việc nào đang bật");
        }

        // 2. Lấy danh sách nhân viên
        List<NhanVien> nhanViens;
        if (req.maNhanViens() != null && !req.maNhanViens().isEmpty()) {
            nhanViens = nvRepo.findByTrangThaiTrue().stream()
                .filter(nv -> req.maNhanViens().contains(nv.getMaNhanVien()))
                .toList();
        } else {
            nhanViens = nvRepo.findByTrangThaiTrue();
        }
        if (nhanViens.isEmpty()) {
            return new XepTuDongResponse("khongUngVien", 0, 0, Map.of(), "Không có nhân viên nào sẵn sàng để xếp ca");
        }

        List<Long> caIds = cas.stream().map(CaLam::getId).toList();

        // 3. Nếu ghi đè -> xóa lịch cũ trong khoảng thời gian đã chọn
        if (Boolean.TRUE.equals(req.ghiDe())) {
            lichRepo.deleteByNgayLamBetweenAndCaLamIdIn(tuNgay, denNgay, caIds);
        }

        // 4. Lấy lịch hiện có trong khoảng thời gian để tránh trùng ca
        List<LichLamViec> lichHienTai = Boolean.TRUE.equals(req.ghiDe())
            ? List.of()
            : lichRepo.findByNgayLamBetween(tuNgay, denNgay);

        Set<String> assignedKeys = new HashSet<>();
        Map<Long, Integer> totalCaCountByNv = new HashMap<>();
        Map<String, Integer> dailyCaCountByNv = new HashMap<>();

        for (NhanVien nv : nhanViens) {
            totalCaCountByNv.put(nv.getId(), 0);
        }

        for (LichLamViec l : lichHienTai) {
            Long nvId = l.getNhanVien().getId();
            assignedKeys.add(nvId + "_" + l.getNgayLam() + "_" + l.getCaLam().getId());
            dailyCaCountByNv.merge(nvId + "_" + l.getNgayLam(), 1, Integer::sum);
            totalCaCountByNv.merge(nvId, 1, Integer::sum);
        }

        int maxMoiNgay = req.maxCaMoiNgay() != null && req.maxCaMoiNgay() > 0 ? req.maxCaMoiNgay() : 1;
        Integer soNvMoiCa = req.soNhanVienMoiCa();

        List<LichLamViec> moi = new ArrayList<>();
        Map<String, Integer> phanBoMoi = new HashMap<>();

        long soNgay = ChronoUnit.DAYS.between(tuNgay, denNgay) + 1;
        LocalDate cur = tuNgay;
        int roundRobinIndex = 0;

        while (!cur.isAfter(denNgay)) {
            LocalDate ngay = cur;

            if (soNvMoiCa == null || soNvMoiCa <= 0) {
                // Chế độ chia đều toàn bộ ứng viên chưa có lịch vào các ca
                List<NhanVien> chuaCoLich = nhanViens.stream()
                    .filter(nv -> cas.stream().noneMatch(c -> assignedKeys.contains(nv.getId() + "_" + ngay + "_" + c.getId())))
                    .toList();

                for (NhanVien nv : chuaCoLich) {
                    CaLam ca = cas.get(roundRobinIndex % cas.size());
                    LichLamViec l = new LichLamViec();
                    l.setNhanVien(nv);
                    l.setCaLam(ca);
                    l.setNgayLam(ngay);
                    moi.add(l);
                    assignedKeys.add(nv.getId() + "_" + ngay + "_" + ca.getId());
                    totalCaCountByNv.merge(nv.getId(), 1, Integer::sum);
                    phanBoMoi.merge(nv.getMaNhanVien(), 1, Integer::sum);
                    roundRobinIndex++;
                }
            } else {
                // Chế độ chỉ định số nhân viên cho mỗi ca
                for (CaLam ca : cas) {
                    long currentInShift = nhanViens.stream()
                        .filter(nv -> assignedKeys.contains(nv.getId() + "_" + ngay + "_" + ca.getId()))
                        .count();

                    int need = (int) Math.max(0, soNvMoiCa - currentInShift);
                    if (need == 0) continue;

                    List<NhanVien> candidates = nhanViens.stream()
                        .filter(nv -> !assignedKeys.contains(nv.getId() + "_" + ngay + "_" + ca.getId()))
                        .filter(nv -> dailyCaCountByNv.getOrDefault(nv.getId() + "_" + ngay, 0) < maxMoiNgay)
                        .sorted(Comparator.comparingInt((NhanVien nv) -> totalCaCountByNv.getOrDefault(nv.getId(), 0))
                            .thenComparing(NhanVien::getId))
                        .toList();

                    int pickCount = Math.min(need, candidates.size());
                    for (int p = 0; p < pickCount; p++) {
                        NhanVien nv = candidates.get(p);
                        LichLamViec l = new LichLamViec();
                        l.setNhanVien(nv);
                        l.setCaLam(ca);
                        l.setNgayLam(ngay);
                        moi.add(l);

                        assignedKeys.add(nv.getId() + "_" + ngay + "_" + ca.getId());
                        dailyCaCountByNv.merge(nv.getId() + "_" + ngay, 1, Integer::sum);
                        totalCaCountByNv.merge(nv.getId(), 1, Integer::sum);
                        phanBoMoi.merge(nv.getMaNhanVien(), 1, Integer::sum);
                    }
                }
            }

            cur = cur.plusDays(1);
        }

        if (moi.isEmpty()) {
            return new XepTuDongResponse("khongUngVien", 0, (int) soNgay, Map.of(), "Không có nhân viên trống hoặc các ca đã đủ định mức");
        }

        lichRepo.saveAll(moi);

        String thongBao = String.format("Đã xếp tự động thành công %d lượt phân ca cho %d nhân viên trong %d ngày (%s đến %s)",
            moi.size(), phanBoMoi.size(), soNgay, tuNgay, denNgay);

        return new XepTuDongResponse(null, moi.size(), (int) soNgay, phanBoMoi, thongBao);
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
