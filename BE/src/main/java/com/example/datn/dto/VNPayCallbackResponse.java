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
public class VNPayCallbackResponse {
    private boolean success;
    private String message;
    private Long orderId;
    private String maHoaDon;
    private BigDecimal amount;
    private String bankCode;
    private String transactionNo;
    private String responseCode;
    private String payDate;
}

