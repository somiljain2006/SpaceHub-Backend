package org.spacehub.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

@Service
public class OTPService {

    @Autowired
    private JavaMailSender mailSender;

    private final String DEFAULT_EMAIL = "monuchaudharypoonia@gmail.com";
    private final Map<String, String> otpMap = new HashMap<>();
    private final Map<String, Instant> sendTimeMap = new HashMap<>();

    private static final int expirySeconds = 300;
    private static final int cooldownSeconds = 60;

    public void sendOTP(String email) {
        if (email == null || email.isEmpty()) {
            email = DEFAULT_EMAIL;
        }

        int num = new Random().nextInt(1000000);
        String otp = String.format("%06d", num);

        otpMap.put(email, otp);
        sendTimeMap.put(email, Instant.now());

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(DEFAULT_EMAIL);
        message.setTo(email);
        message.setSubject("OTP Verification");
        message.setText("Your OTP is: " + otp);

        mailSender.send(message);
    }

    public boolean validateOTP(String otp) {
        return otpMap.containsValue(otp);
    }

    public boolean canSendOTP(String email) {
        Instant lastSent = sendTimeMap.get(email);
        if (lastSent == null) return true;
        return Instant.now().isAfter(lastSent.plusSeconds(cooldownSeconds));
    }


    public long cooldownTime(String email) {
        Instant lastSent = sendTimeMap.get(email);
        if (lastSent == null) return 0;
        long diff = Instant.now().getEpochSecond() - lastSent.getEpochSecond();
        return Math.max(0, cooldownSeconds - diff);
    }
}
