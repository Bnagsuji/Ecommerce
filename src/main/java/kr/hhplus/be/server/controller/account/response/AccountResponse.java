package kr.hhplus.be.server.controller.account.response;

import kr.hhplus.be.server.domain.account.Account;

public record AccountResponse(
        Long userId,
        Long amount
) {
    public static AccountResponse from(Account account) {
        return new AccountResponse(account.getUserId(), account.getAmount());
    }
}
