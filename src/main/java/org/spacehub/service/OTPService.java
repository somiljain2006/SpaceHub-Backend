package org.spacehub.service;

import org.spacehub.entities.OTP;
import org.spacehub.repository.OTPRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class OTPService {

    @Autowired
    private JavaMailSender mailSender;

    @Autowired
    private OTPRepository otpRepository;

    private final String DEFAULT_EMAIL = "monuchaudharypoonia@gmail.com";
    private static final int expirySeconds = 300;
    private static final int cooldownSeconds = 60;

    public void sendOTP(String email) {
        if (email == null || email.isEmpty()) {
            email = DEFAULT_EMAIL;
        }

        int num = new Random().nextInt(1000000);
        String otpCode = String.format("%06d", num);
        Instant now = Instant.now();

        OTP otp = new OTP();
        otp.setEmail(email);
        otp.setCode(otpCode);
        otp.setCreatedAt(now);
        otp.setExpiresAt(now.plusSeconds(expirySeconds));

        otpRepository.save(otp);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(DEFAULT_EMAIL);
        message.setTo(email);
        message.setSubject("OTP Verification");
        message.setText("Your OTP is: " + otpCode); // send the code, not the OTP object

        mailSender.send(message);
    }

    public boolean validateOTP(String email, String otpCode) {
        Optional<OTP> otpOptional = otpRepository.findByEmailAndCode(email, otpCode);
        return otpOptional.map(otp -> Instant.now().isBefore(otp.getExpiresAt())).orElse(false);
    }

    public boolean canSendOTP(String email) {
        Optional<OTP> lastOtp = otpRepository.findTopByEmail(email);
        return lastOtp.map(otp -> Duration.between(otp.getCreatedAt(), Instant.now()).getSeconds() >= cooldownSeconds)
                .orElse(true);
    }

    public long cooldownTime(String email) {
        Optional<OTP> lastOtp = otpRepository.findTopByEmail(email);
        return lastOtp.map(otp -> {
                    long elapsed = Duration.between(otp.getCreatedAt(), Instant.now()).getSeconds();
                    return Math.max(0, cooldownSeconds - elapsed);
                })
                .orElse(0L);
    }
}
