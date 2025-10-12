package org.spacehub.repository;

import org.spacehub.security.ConfirmationToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
@Transactional(readOnly = true)
public interface ConfirmationTokenRepository
  extends JpaRepository<ConfirmationToken, Long> {
}
