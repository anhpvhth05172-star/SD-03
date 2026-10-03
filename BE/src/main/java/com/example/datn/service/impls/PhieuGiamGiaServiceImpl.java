package com.example.datn.service.impls;

import com.example.datn.dto.PhieuGiamGiaDTO;
import com.example.datn.repository.PhieuGiamGiaRepository;
import com.example.datn.service.PhieuGiamGiaService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
public class PhieuGiamGiaServiceImpl implements PhieuGiamGiaService {

    @Autowired
    private PhieuGiamGiaRepository phieuGiamGiaRepository;

    @Override
    public Page<PhieuGiamGiaDTO> getPageList(Pageable pageable) {
        return phieuGiamGiaRepository.getListPageDiscount(pageable);
    }
}
