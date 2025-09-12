package kr.hhplus.be.server.service.account.impl;

import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.domain.account.AccountRepository;
import kr.hhplus.be.server.infrastructure.repository.account.AccountHistoryRepository;
import kr.hhplus.be.server.lock.DistributedLock;
import kr.hhplus.be.server.service.account.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.function.Consumer;

@Service
@RequiredArgsConstructor
public class AccountServiceImpl implements AccountService {

    private final AccountRepository accountRepository;
    private final AccountHistoryRepository accountHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getBalance(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseGet(() -> Account.createNew(userId)); // 0원 가정 (또는 save로 실제 생성)
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

    private AccountResponse processBalanceWithLock(
            Long userId, Long amount, TransactionType type, Consumer<Account> action) {

        Account account = accountRepository.findByUserIdForUpdate(userId)
                .orElseGet(() -> accountRepository.save(Account.createNew(userId)));

        action.accept(account);

        accountHistoryRepository.save(
                kr.hhplus.be.server.domain.account.AccountHistory.create(
                        userId, amount, type, LocalDateTime.now()
                )
        );

        return new AccountResponse(account.getUserId(), account.getAmount());
    }
}
