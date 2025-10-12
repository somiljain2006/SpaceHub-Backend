package org.spacehub.DTO;

import lombok.Data;

@Data
public class OTPRequest {

    private String email;
    private String otp;

}
