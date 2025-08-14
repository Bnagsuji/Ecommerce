package kr.hhplus.be.server.controller.account.response;

import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.domain.account.AccountHistory;

import java.time.LocalDateTime;

public record AccountHistoryResponse(
        Long userId, // "CHARGE" or "USE"z
        Long amount,
        TransactionType type,
        LocalDateTime createdDate
) {

    public static AccountHistoryResponse from(AccountHistory history) {
        return new AccountHistoryResponse(
                history.getUserId(),
                history.getAmount(),
                history.getType(),
                history.getCreatedDate()
        );
    }
}
