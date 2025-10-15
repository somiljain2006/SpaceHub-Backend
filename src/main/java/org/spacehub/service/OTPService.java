package org.spacehub.service;

import org.spacehub.entities.OTP;
import org.spacehub.entities.OtpType;
import org.spacehub.repository.OTPRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import java.util.Random;

@Service
public class OTPService {

  private final JavaMailSender mailSender;
  private final OTPRepository otpRepository;
  private final String defaultEmail;

  private static final int EXPIRY_SECONDS = 300;
  private static final int COOLDOWN_SECONDS = 30;

  public OTPService(JavaMailSender mailSender,
                    OTPRepository otpRepository,
                    @Value("${spring.mail.username}") String defaultEmail) {
    this.mailSender = mailSender;
    this.otpRepository = otpRepository;
    this.defaultEmail = defaultEmail;
  }

  public void sendOTP(String email, OtpType type) {

    int num = new Random().nextInt(1000000);
    String otpCode = String.format("%06d", num);
    Instant now = Instant.now();

    OTP otp = new OTP();
    otp.setEmail(email);
    otp.setCode(otpCode);
    otp.setCreatedAt(now);
    otp.setExpiresAt(now.plusSeconds(EXPIRY_SECONDS));
    otp.setType(type);
    otp.setUsed(false);
    otpRepository.save(otp);

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(defaultEmail);
    message.setTo(email);
    message.setSubject("OTP Verification");
    message.setText("Your OTP is: " + otpCode);

    mailSender.send(message);
  }

  public boolean validateOTP(String email, String otpCode, OtpType type) {
    Optional<OTP> otpOptional = otpRepository.findByEmailAndCodeAndType(email, otpCode, type);
    boolean valid = otpOptional.map(otp -> Instant.now().isBefore(otp.getExpiresAt())).orElse(false);

    if (valid) {
      otpOptional.ifPresent(otp -> {
        otp.setUsed(true);
        otpRepository.save(otp);
      });
    }
    return valid;
  }

  public boolean isInCooldown(String email, OtpType type) {
    Optional<OTP> lastOtp = otpRepository.findTopByEmailAndType(email, type);
    if (lastOtp.isEmpty()) {
      return false;
    }
    long elapsed = Duration.between(lastOtp.get().getCreatedAt(), Instant.now()).getSeconds();
    return elapsed < COOLDOWN_SECONDS;
  }

  public long cooldownTime(String email, OtpType type) {
    Optional<OTP> lastOtp = otpRepository.findTopByEmailAndType(email, type);
    return lastOtp.map(otp -> {
      long elapsed = Duration.between(otp.getCreatedAt(), Instant.now()).getSeconds();
      return Math.max(0, COOLDOWN_SECONDS - elapsed);
    }).orElse(0L);
  }
}
