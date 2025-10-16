package org.spacehub.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.spacehub.entities.OtpType;
import org.spacehub.entities.RegistrationRequest;
import org.springframework.stereotype.Service;
import java.security.SecureRandom;

@Service
public class OTPService {

  private final RedisService redisService;
  private final EmailService emailService;
  private static final SecureRandom random = new SecureRandom();
  private final ObjectMapper objectMapper = new ObjectMapper();

  private static final int OTP_EXPIRE_SECONDS = 300;
  private static final int COOLDOWN_SECONDS = 30;
  private static final int TEMP_REGISTRATION_EXPIRE = 600;
  private static final int MAX_ATTEMPTS = 3;
  private static final int BLOCK_DURATION = 300;

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

    emailService.sendEmail(email, "Your OTP is: " + otp + ". It will expire in 5 minutes.");
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

    String attemptKey = "OTP_ATTEMPTS_" + type + "_" + email;
    redisService.deleteValue(attemptKey);
  }

  public boolean isInCooldown(String email, OtpType type) {
    String key = "OTP_COOLDOWN_" + type + "_" + email;
    Long liveTime = redisService.getLiveTime(key);
    return liveTime != null && liveTime > 0;
  }

  public long cooldownTime(String email, OtpType type) {
    String key = "OTP_COOLDOWN_" + type + "_" + email;
    Long liveTime = redisService.getLiveTime(key);
    return (liveTime != null && liveTime > 0) ? liveTime : 0;
  }

  public void saveTempOtp(String email, RegistrationRequest request) {
    try {
      String convertedKey = objectMapper.writeValueAsString(request);
      String key = "REGISTRATION_TEMP_" + email;
      redisService.saveValue(key, convertedKey, TEMP_REGISTRATION_EXPIRE);
    }
    catch (Exception e) {
      throw new RuntimeException("Failed to save temporary registration", e);
    }
  }

  public RegistrationRequest getTempOtp(String email) {
    try {
      String key = "REGISTRATION_TEMP_" + email;
      String json = redisService.getValue(key);
      if (json == null) return null;
      return objectMapper.readValue(json, RegistrationRequest.class);
    } catch (Exception e) {
      return null;
    }
  }

  public void deleteTempOtp(String email) {
    String key = "REGISTRATION_TEMP_" + email;
    redisService.deleteValue(key);
  }

  public long incrementOtpAttempts(String email, OtpType type) {
    String attemptKey = "OTP_ATTEMPTS_" + type + "_" + email;
    Long attempts = redisService.incrementValue(attemptKey);
    if (attempts == 1) redisService.setExpiry(attemptKey, BLOCK_DURATION);
    return attempts;
  }

  public void blockOtp(String email, OtpType type) {
    String blockKey = "OTP_BLOCKED_" + type + "_" + email;
    redisService.saveValue(blockKey, "1", BLOCK_DURATION);
  }

  public boolean isBlocked(String email, OtpType type) {
    String blockKey = "OTP_BLOCKED_" + type + "_" + email;
    return redisService.exists(blockKey);
  }

}
