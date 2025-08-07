// AccountService.java
package kr.hhplus.be.server.service.account;

import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;

import java.util.List;

public interface AccountService {

    // 잔액 조회
    AccountResponse getBalance(Long userId);

    // 잔액 충전
    AccountResponse chargeBalance(Long userId, Long chargeAmount);

    // 잔액 사용
    AccountResponse useBalance(Long userId, Long useAmount);

    // 거래 내역 조회
    List<AccountHistoryResponse> getHistory(Long userId);
}

