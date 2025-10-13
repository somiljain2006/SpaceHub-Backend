package org.spacehub.repository;

import org.spacehub.entities.OTP;
import org.spacehub.entities.OtpType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Long> {

  Optional<OTP> findTopByEmailAndType(String email, OtpType type);

  Optional<OTP> findByEmailAndCodeAndType(String email, String code, OtpType type);
}
