package kr.hhplus.be.server.service.account;

import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.controller.account.response.AccountHistoryResponse;
import kr.hhplus.be.server.controller.account.response.AccountResponse;

import java.util.List;

public interface AccountService {

    AccountResponse getBalance(Long userId);

    AccountResponse chargeBalance(Long userId, Long chargeAmount);

    AccountResponse useBalance(Long userId, Long useAmount);

    AccountHistoryResponse saveHistory(AccountResponse res, TransactionType type);

    List<AccountHistoryResponse> getHistory(Long userId);
}
