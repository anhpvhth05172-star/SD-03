package com.example.datn.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.datn.dto.ThuocTinh.AttributeRequest;
import com.example.datn.entity.KichCo;
import com.example.datn.repository.KichCoRepository;
import com.example.datn.service.impl.KichCoServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class KichCoServiceImplTest {

    private KichCoRepository repository;
    private KichCoServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = mock(KichCoRepository.class);
        service = new KichCoServiceImpl(repository);
        when(repository.existsByTenKichCoIgnoreCase(any())).thenReturn(false);
        when(repository.count()).thenReturn(0L);
        when(repository.save(any(KichCo.class))).thenAnswer(call -> call.getArgument(0));
    }

    @Test
    void acceptsEuWholeAndHalfSizesAndNormalizesDecimalComma() {
        var response = service.create(new AttributeRequest(null, " 38,50 ", null, true));

        assertThat(response.getTen()).isEqualTo("38.5");
    }

    @Test
    void rejectsTextAndNonPositiveSize() {
        for (String size : new String[] {"XL", "0", "-38"}) {
            assertThatThrownBy(() -> service.create(new AttributeRequest(null, size, null, true)))
                .isInstanceOf(IllegalArgumentException.class);
        }
        verify(repository, never()).save(any(KichCo.class));
    }
}
