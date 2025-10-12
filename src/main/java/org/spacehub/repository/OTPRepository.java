package org.spacehub.repository;

import org.spacehub.entities.OTP;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface OTPRepository extends JpaRepository<OTP, Long> {

  Optional<OTP> findTopByEmail(String email);

  Optional<OTP> findByEmailAndCode(String email, String code);
}
