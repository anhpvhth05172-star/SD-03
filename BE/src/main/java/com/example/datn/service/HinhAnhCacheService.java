package com.example.datn.service;

import com.example.datn.entity.HinhAnhSanPham;
import com.example.datn.repository.HinhAnhSanPhamRepository;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class HinhAnhCacheService {

    private final HinhAnhSanPhamRepository hinhAnhSanPhamRepository;
    private final Map<Long, List<HinhAnhSanPham>> cache = new ConcurrentHashMap<>();

    public List<HinhAnhSanPham> getImagesBySanPhamId(Long idSanPham) {
        if (idSanPham == null) return Collections.emptyList();
        return cache.computeIfAbsent(idSanPham, id -> {
            try {
                return hinhAnhSanPhamRepository.findBySanPhamId(id);
            } catch (Exception e) {
                log.error("Loi khi doc anh san pham {}: {}", id, e.getMessage());
                return Collections.emptyList();
            }
        });
    }

    public Map<Long, List<HinhAnhSanPham>> getImagesBySanPhamIdIn(Collection<Long> idSanPhams) {
        if (idSanPhams == null || idSanPhams.isEmpty()) return Collections.emptyMap();

        Map<Long, List<HinhAnhSanPham>> result = new HashMap<>();
        List<Long> missingIds = new ArrayList<>();

        for (Long id : idSanPhams) {
            if (id == null) continue;
            List<HinhAnhSanPham> cached = cache.get(id);
            if (cached != null) {
                result.put(id, cached);
            } else {
                missingIds.add(id);
            }
        }

        if (!missingIds.isEmpty()) {
            try {
                List<HinhAnhSanPham> fetched = hinhAnhSanPhamRepository.findBySanPhamIdIn(missingIds);
                Map<Long, List<HinhAnhSanPham>> grouped = fetched.stream()
                        .filter(img -> img.getSanPham() != null && img.getSanPham().getId() != null)
                        .collect(Collectors.groupingBy(img -> img.getSanPham().getId()));

                for (Long missingId : missingIds) {
                    List<HinhAnhSanPham> list = grouped.getOrDefault(missingId, Collections.emptyList());
                    cache.put(missingId, list);
                    result.put(missingId, list);
                }
            } catch (Exception e) {
                log.error("Loi khi batch doc anh san pham: {}", e.getMessage());
            }
        }

        return result;
    }

    public void invalidate(Long idSanPham) {
        if (idSanPham != null) {
            cache.remove(idSanPham);
        }
    }

    public void invalidateAll() {
        cache.clear();
    }
}

