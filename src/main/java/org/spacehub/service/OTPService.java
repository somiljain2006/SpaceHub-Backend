package org.spacehub.service;

import org.spacehub.entities.OtpType;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OTPService {

  private final RedisService redisService;
  private final EmailService emailService;
  private static final SecureRandom random = new SecureRandom();
  private static final int OTP_EXPIRE_SECONDS = 300;
  private static final int COOLDOWN_SECONDS = 30;

  public OTPService(RedisService redisService, EmailService emailService) {
    this.redisService = redisService;
    this.emailService = emailService;
  }

  private String generateOtp() {
    int otp = 100000 + random.nextInt(900000);
    return String.valueOf(otp);
  }

  public void sendOTP(String email, OtpType type) {
    String otp = generateOtp();
    String otpKey = "OTP_" + type + "_" + email;
    String cooldownKey = "OTP_COOLDOWN_" + type + "_" + email;
    redisService.saveValue(otpKey, otp, OTP_EXPIRE_SECONDS);
    redisService.saveValue(cooldownKey, "1", COOLDOWN_SECONDS);
    emailService.sendEmail(email, "Your OTP is: " + otp);
  }

  public boolean validateOTP(String email, String otp, OtpType type) {
    String key = "OTP_" + type + "_" + email;
    String savedOtp = redisService.getValue(key);
    return savedOtp != null && savedOtp.equals(otp);
  }

  public boolean isUsed(String email, OtpType type) {
    String key = "OTP_USED_" + type + "_" + email;
    return redisService.exists(key);
  }

  public void markAsUsed(String email, String otp, OtpType type) {
    String usedKey = "OTP_USED_" + type + "_" + email;
    redisService.saveValue(usedKey, otp, OTP_EXPIRE_SECONDS);
    String otpKey = "OTP_" + type + "_" + email;
    redisService.deleteValue(otpKey);
  }

  public boolean isInCooldown(String email, OtpType type) {
    String key = "OTP_COOLDOWN_" + type + "_" + email;
    return redisService.exists(key);
  }

  public long cooldownTime(String email, OtpType type) {
    return isInCooldown(email, type) ? COOLDOWN_SECONDS : 0;
  }
}
