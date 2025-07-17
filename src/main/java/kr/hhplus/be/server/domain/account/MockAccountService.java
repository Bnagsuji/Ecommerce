package kr.hhplus.be.server.domain.account;

import kr.hhplus.be.server.domain.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.domain.account.response.AccountResponse;
import kr.hhplus.be.server.domain.order.request.OrderRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MockAccountService {

    private final Map<Long, Integer> balances = new ConcurrentHashMap<>();
    public MockAccountService() {
        // 회원 잔액
        balances.put(1L, 5_000_000);
        balances.put(2L, 100_000);
    }

    private final Map<Long, List<AccountHistoryResponse>> histories = new ConcurrentHashMap<>();
    private final Map<Long, Object> locks = new ConcurrentHashMap<>();

    private Object getLock(Long memberId) {
        return locks.computeIfAbsent(memberId, k -> new Object());
    }

    public AccountResponse chargeBalance(Long memberId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("충전 금액은 0보다 커야 합니다.");
        }

        synchronized (getLock(memberId)) {
            int current = balances.getOrDefault(memberId, 0);
            int updated = current + amount;
            balances.put(memberId, updated);

            histories.computeIfAbsent(memberId, k -> new ArrayList<>())
                    .add(new AccountHistoryResponse("CHARGE", amount, LocalDateTime.now()));

            return new AccountResponse(memberId, updated);
        }
    }

    public AccountResponse deductBalance(Long memberId, int amount) {
        if (amount <= 0) {
            throw new IllegalArgumentException("차감 금액은 0보다 커야 합니다.");
        }

        synchronized (getLock(memberId)) {
            int current = balances.getOrDefault(memberId, 0);
            if (current < amount) {
                throw new IllegalStateException("잔액이 부족합니다.");
            }
            int updated = current - amount;
            balances.put(memberId, updated);

            histories.computeIfAbsent(memberId, k -> new ArrayList<>())
                    .add(new AccountHistoryResponse("USE", amount, LocalDateTime.now()));

            return new AccountResponse(memberId, updated);
        }
    }

    public AccountResponse getBalance(Long memberId) {
        int balance = balances.getOrDefault(memberId, 2000);
        return new AccountResponse(memberId, balance);
    }

    public List<AccountHistoryResponse> getHistory(Long memberId) {
        return histories.getOrDefault(memberId, Collections.emptyList());
    }

    public void rollback(Long memberId) {
        // 단순하게 "차감된 금액을 다시 복구"한다고 가정 (여기선 로그만)
        System.out.println("잔액 롤백 처리: memberId = " + memberId);
    }


}
