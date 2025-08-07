package kr.hhplus.be.server.controller.account.request;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class AccountHistoryRequest {
    private Long userId;
    private Long amount;
    private TransactionType type;
}
