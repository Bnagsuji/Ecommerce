package kr.hhplus.be.server.domain.account;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class AccountHistory {
    private final Long memberId;
    private final int amount;
    private final LocalDateTime createdAt;

    public AccountHistory(Long memberId, int amount) {
        this.memberId = memberId;
        this.amount = amount;
        this.createdAt = LocalDateTime.now();
    }
}
