package kr.hhplus.be.server.infrastructure.repository.account;

import kr.hhplus.be.server.domain.account.Account;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AccountJpaRepository extends JpaRepository<Account, Long> {
    Optional<Account> findByUserId(Long userId);
}
