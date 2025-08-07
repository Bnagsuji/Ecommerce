package kr.hhplus.be.server.service.account.impl;

import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.domain.account.AccountRepository;
import kr.hhplus.be.server.infrastructure.repository.account.AccountHistoryRepository;
import kr.hhplus.be.server.infrastructure.repository.account.AccountJpaRepository;
import kr.hhplus.be.server.service.account.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Consumer;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {
    //내 계좌 서비스 : 잔액 조회, 잔액 차감(결제 성공 시), 잔액 충전, 계좌 히스토리 저장

    private final AccountRepository accountRepository;
    private final AccountHistoryRepository accountHistoryRepository;

    @Override
    @Transactional(readOnly = true)
    public AccountResponse getBalance(Long userId) {
        Account account = accountRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));
        return account.toResponse();
    }

    @Override
    @Transactional
    public AccountResponse chargeBalance(Long userId, Long chargeAmount) {
        return processBalance(userId, chargeAmount, TransactionType.CHARGE, a -> a.charge(chargeAmount));
    }

    @Override
    @Transactional
    public AccountResponse useBalance(Long userId, Long useAmount) {
        return processBalance(userId, useAmount, TransactionType.USE, a -> a.use(useAmount));
    }


    @Override
    @Transactional(readOnly = true)
    public List<AccountHistoryResponse> getHistory(Long userId) {
        return accountHistoryRepository.findByUserId(userId).stream()
                .map(h -> new AccountHistoryResponse(
                        h.getUserId(),
                        h.getAmount(),
                        h.getType(),
                        h.getCreatedDate()
                )).toList();
    }

    private AccountResponse processBalance(Long userId, Long amount, TransactionType type, Consumer<Account> action) {
        Account account = accountRepository.findByUserId(userId)
                .orElseGet(() -> Account.builder().userId(userId).amount(0L).build());

        action.accept(account); // 충전 or 사용 처리
        accountHistoryRepository.save(account.toHistory(type));
        return account.toResponse();
    }
}
