package kr.hhplus.be.server.service.account.impl;

import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.domain.account.AccountRepository;
import kr.hhplus.be.server.infrastructure.repository.account.AccountHistoryRepository;
import kr.hhplus.be.server.lock.DistributedLock;
import kr.hhplus.be.server.service.account.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {
    private final AccountRepository accountRepository;
    private final AccountHistoryRepository accountHistoryRepository;

    @Override
    public AccountResponse getBalance(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return new AccountResponse(account.getUserId(), account.getAmount());
    }

    @Override
    @DistributedLock(
            prefix = "lock:account",                    // Redis 락 키의 prefix
            keys = { "#userId" },
            lease = 5,
            unit = ChronoUnit.SECONDS,
            waitFor = 10,
            waitUnit = ChronoUnit.SECONDS,
            pollMillis = 100
    )
    @Transactional
    public AccountResponse chargeBalance(Long userId, Long chargeAmount) {
        return processBalanceWithLock(userId, chargeAmount, TransactionType.CHARGE, a -> a.charge(chargeAmount));
    }

    @Override
    @DistributedLock(
            prefix = "lock:account",                    // Redis 락 키의 prefix
            keys = { "#userId" },
            lease = 10,
            unit = ChronoUnit.SECONDS,
            waitFor = 5,
            waitUnit = ChronoUnit.SECONDS,
            pollMillis = 50
    )
    @Transactional(propagation = Propagation.REQUIRED)
    public AccountResponse useBalance(Long userId, Long useAmount) {
        return processBalanceWithLock(userId, useAmount, TransactionType.USE, a -> a.use(useAmount));
    }

    @Override
    @Transactional(readOnly = true)
    public List<AccountHistoryResponse> getHistory(Long userId) {
        return accountHistoryRepository.findByUserId(userId).stream()
                .map(AccountHistoryResponse::from)
                .toList();
    }

    private AccountResponse processBalanceWithLock(Long userId, Long amount, TransactionType type, Consumer<Account> action) {
        Account account = accountRepository.findByUserId(userId)
                .orElseGet(() -> {
                    Account newAccount = Account.createNew(userId);
                    return accountRepository.save(newAccount);
                });

        action.accept(account);

        return new AccountResponse(account.getUserId(), account.getAmount());
    }
}
