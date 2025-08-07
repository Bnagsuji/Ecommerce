package kr.hhplus.be.server.infrastructure.repository.account;

import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.domain.account.AccountRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
@AllArgsConstructor
public class AccountRepositoryImpl implements AccountRepository {

    private final AccountJpaRepository accountJpaRepository;

    @Override
    public Optional<Account> findByUserId(Long userId) {
        return accountJpaRepository.findByUserId(userId);
    }

    @Override
    public Optional<Account> findByUserIdForUpdate(Long userId) {
        return accountJpaRepository.findByUserIdForUpdate(userId);
    }

    @Override
    public Account save(Account account) {
        return accountJpaRepository.save(account);
    }

}
