package com.example.datn.dto;

import java.math.BigDecimal;
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
public class VNPayPaymentResponse {
    private String status;
    private String message;
    private String paymentUrl;
    private Long orderId;
    private String maHoaDon;
    private BigDecimal amount;
    private String qrUrl;
}

