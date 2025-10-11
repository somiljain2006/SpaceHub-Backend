package org.spacehub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Random;

@Service
public class OTPService {

    @Autowired
    private JavaMailSender mailSender;

    private final String DEFAULT_EMAIL = "monuchaudharypoonia@gmail.com";

    private String prev_OTP;
    private Instant send_Time;

    public void sendOTP(String email) {
        if (email == null || email.isEmpty()) {
            email = DEFAULT_EMAIL;
        }

        int num = new Random().nextInt(1000000);
        String otp = String.format("%06d", num);

        this.prev_OTP = otp;
        this.send_Time = Instant.now();

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(DEFAULT_EMAIL);
        message.setTo(email);
        message.setSubject("OTP Verification");
        message.setText("Your OTP is: " + otp);

        mailSender.send(message);
    }

    public boolean validateOTP(String otp) {
        if (prev_OTP == null || send_Time == null) {
            return false;
        }

        long timeTakenMillis = Instant.now().toEpochMilli() - send_Time.toEpochMilli();

        if (timeTakenMillis > 3000000) {
            this.prev_OTP = null;
            this.send_Time = null;
            return false;
        }

        boolean isValid = prev_OTP.equals(otp);

        if (isValid) {
            this.prev_OTP = null;
            this.send_Time = null;
        }

        return isValid;
    }
}
