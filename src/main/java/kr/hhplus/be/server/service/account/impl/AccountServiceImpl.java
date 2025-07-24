package kr.hhplus.be.server.service.account.impl;

import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.repository.account.AccountRepository;
import kr.hhplus.be.server.service.account.AccountService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
@AllArgsConstructor
public class AccountServiceImpl implements AccountService {
    //내 계좌 서비스 : 잔액 조회, 잔액 차감(결제 성공 시), 잔액 충전, 계좌 히스토리 저장

    private final AccountRepository accountRepository;

    //유저별 잔액 정보
    private final Map<Long, AccountResponse> myAccount = new HashMap<>(Map.of(
            1L, new AccountResponse(1L, 200L),
            2L, new AccountResponse(2L, 300L)
    ));
    private final Map<Long, List<AccountHistoryResponse>> histories = new ConcurrentHashMap<>();

    /* 잔액 조회 */
    @Override
    public AccountResponse getBalance(Long userId) {
        if(!myAccount.containsKey(userId)) {
            throw new IllegalArgumentException("존재하지 않는 사용자입니다.");
        }
        long res = myAccount.get(userId).amount();

        return new AccountResponse(userId, res);
    }

    /* 잔액 충전 */
    @Override
    public AccountResponse chargeBalance(Long userId, Long chargeAmount) {
        //충전 시 100L 미만의 금액 충전 불가
        if(chargeAmount < 100L) {
            throw new IllegalArgumentException("최소 잔액 충전 정책에 맞지 않는 금액입니다.");
        }
        //한 번에 100000L를 초과하는 금액 충전 불가
        if(chargeAmount > 100000L) {
            throw new IllegalArgumentException("최대 잔액 충전 정책에 맞지 않는 금액입니다.");
        }

        AccountResponse currentAccount = myAccount.getOrDefault(userId, new AccountResponse(userId, 0L));

        // 새로운 잔액 계산.
        Long newBalance = currentAccount.amount() + chargeAmount;

        //히스토리 저장

        return new AccountResponse(userId, newBalance);
    }


    /* 잔액 사용 */
    @Override
    public AccountResponse useBalance(Long userId, Long useAmount) {
        //사용자 포인트 가져 와야 함
        Long currentAmount = myAccount.get(userId).amount();

        if(useAmount < 100L) {
            throw new IllegalArgumentException("현재 100원 부터 사용 가능합니다.");
        }

        if(useAmount > currentAmount) {
            throw new IllegalArgumentException("사용하려는 금액이 현재 보유 중인 금액보다 많습니다.");
        }

        // 새로운 잔액 계산.
        Long newBalance = currentAmount - useAmount;

        //히스토리 저장


        return new AccountResponse(userId, newBalance);
    }

    /* 히스토리 저장 */
    @Override
    public AccountHistoryResponse saveHistory(AccountResponse res, TransactionType type) {
        if(!myAccount.containsKey(res.userId())){
            throw new IllegalArgumentException("존재하지 않는 사용자 입니다.");
        }
        return new AccountHistoryResponse(res.userId(), res.amount(), type, LocalDateTime.now());
    }

    /* 히스토리 조회 */
    @Override
    public List<AccountHistoryResponse> getHistory(Long userId) {

        return List.of(
                new AccountHistoryResponse(userId,200L, TransactionType.CHARGE, LocalDateTime.now()),
                new AccountHistoryResponse(userId,100L, TransactionType.USE, LocalDateTime.now()),
                new AccountHistoryResponse(userId,200L, TransactionType.CHARGE, LocalDateTime.now())
                );
    }


}
