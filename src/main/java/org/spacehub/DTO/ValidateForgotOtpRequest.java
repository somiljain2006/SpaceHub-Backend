package org.spacehub.DTO;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ValidateForgotOtpRequest {
    private String email;
    private String otp;

    public ValidateForgotOtpRequest() {}
    public ValidateForgotOtpRequest(String email, String otp) {
        this.email = email;
        this.otp = otp;
    }
}