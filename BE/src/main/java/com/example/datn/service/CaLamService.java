package com.example.datn.service;

import com.example.datn.dto.LichLamViec.CaLamDTO;
import com.example.datn.dto.LichLamViec.CaLamRequest;
import com.example.datn.entity.CaLam;
import com.example.datn.exception.BiChanException;
import com.example.datn.exception.DuplicateRecordException;
import com.example.datn.exception.ResourceNotFoundException;
import com.example.datn.repository.CaLamRepository;
import java.text.Normalizer;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CaLamService {

    private final CaLamRepository caLamRepository;

    private static final Pattern NON_ALNUM = Pattern.compile("[^a-z0-9]+");

    @Transactional(readOnly = true)
    public List<CaLamDTO> list() {
        return caLamRepository.findAll().stream()
            .sorted(Comparator.comparing(CaLam::getBatDau))
            .map(CaLamService::toDTO)
            .toList();
    }

    @Transactional
    public CaLamDTO create(CaLamRequest req) {
        validate(req, null);
        if (caLamRepository.existsByTenCaIgnoreCase(req.ten())) {
            throw new DuplicateRecordException("Tên ca làm đã tồn tại, vui lòng chọn tên khác");
        }
        CaLam ca = new CaLam();
        ca.setTenCa(req.ten());
        ca.setBatDau(req.batDau());
        ca.setKetThuc(req.ketThuc());
        ca.setDangBat(true);
        ca.setMaCa(sinhMa(req.ten()));
        ca.setNgayTao(LocalDateTime.now());
        CaLam saved = caLamRepository.save(ca);
        return toDTO(saved);
    }

    @Transactional
    public CaLamDTO update(Long id, CaLamRequest req) {
        CaLam ca = caLamRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca làm"));
        validate(req, id);
        if (caLamRepository.existsByTenCaIgnoreCaseAndIdNot(req.ten(), id)) {
            throw new DuplicateRecordException("Tên ca làm đã tồn tại, vui lòng chọn tên khác");
        }
        ca.setTenCa(req.ten());
        ca.setBatDau(req.batDau());
        ca.setKetThuc(req.ketThuc());
        ca.setNgayCapNhat(LocalDateTime.now());
        CaLam saved = caLamRepository.save(ca);
        return toDTO(saved);
    }

    @Transactional
    public CaLamDTO toggle(Long id) {
        CaLam ca = caLamRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca làm"));
        ca.setDangBat(!ca.getDangBat());
        ca.setNgayCapNhat(LocalDateTime.now());
        CaLam saved = caLamRepository.save(ca);
        return toDTO(saved);
    }

    @Transactional
    public void delete(Long id) {
        CaLam ca = caLamRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy ca làm"));
        long soLich = caLamRepository.demLichTheoCa(id);
        if (soLich > 0) {
            throw new BiChanException(
                "Còn " + soLich + " lịch làm việc đang gắn với ca \"" + ca.getTenCa() + "\" - hãy xóa hoặc đổi các lịch đó trước");
        }
        caLamRepository.deleteById(id);
    }

    private void validate(CaLamRequest req, Long idHienTai) {
        if (req.ten() == null || req.ten().isBlank()) {
            throw new IllegalArgumentException("Tên ca làm không được để trống");
        }
        if (req.batDau() == null || req.ketThuc() == null) {
            throw new IllegalArgumentException("Thời gian bắt đầu và kết thúc không được để trống");
        }
        if (!req.batDau().isBefore(req.ketThuc())) {
            throw new IllegalArgumentException("Thời gian không hợp lệ: bắt đầu phải trước kết thúc");
        }
        List<CaLam> trungKhung = caLamRepository.findAll().stream()
            .filter(c -> !c.getId().equals(idHienTai))
            .filter(c -> c.getBatDau().isBefore(req.ketThuc()) && c.getKetThuc().isAfter(req.batDau()))
            .toList();
        if (!trungKhung.isEmpty()) {
            CaLam trung = trungKhung.get(0);
            throw new DuplicateRecordException(
                "Ca \"" + trung.getTenCa() + "\" đang tồn tại - hãy bỏ ca trước");
        }
    }

    private String sinhMa(String ten) {
        String slug = slug(ten);
        if (slug.isEmpty()) slug = "ca";
        String ma = slug;
        int i = 2;
        while (caLamRepository.existsByMaCa(ma)) {
            ma = slug + "-" + i;
            i++;
        }
        return ma;
    }

    static String slug(String ten) {
        String normalized = Normalizer.normalize(ten.toLowerCase(Locale.ROOT), Normalizer.Form.NFD);
        String noMarks = NON_ALNUM.matcher(normalized.replaceAll("\\p{M}+", "")).replaceAll("-");
        return noMarks.replaceAll("(^-+|-+$)", "");
    }

    static CaLamDTO toDTO(CaLam ca) {
        return new CaLamDTO(ca.getId(), ca.getMaCa(), ca.getTenCa(), ca.getBatDau(), ca.getKetThuc(), ca.getDangBat());
    }
}
