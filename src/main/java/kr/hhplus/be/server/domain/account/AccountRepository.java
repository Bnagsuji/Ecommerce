package kr.hhplus.be.server.domain.account;

import java.util.Optional;

public interface AccountRepository {
    Optional<Account> findByUserId(Long userId);
    Optional<Account> findByUserIdForUpdate(Long userId);
    Account save(Account account);
}
