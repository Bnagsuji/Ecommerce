package kr.hhplus.be.server.domain.account;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByUserId(Long userId);
}
