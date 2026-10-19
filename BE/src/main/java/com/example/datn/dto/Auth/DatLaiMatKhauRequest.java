package com.example.datn.dto.Auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DatLaiMatKhauRequest {

    private String email;
    private String maOTP;
    private String matKhauMoi;
    private String xacNhan;
}
