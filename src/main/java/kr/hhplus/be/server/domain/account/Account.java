package kr.hhplus.be.server.domain.account;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import kr.hhplus.be.server.controller.account.request.TransactionType;
import kr.hhplus.be.server.controller.account.response.AccountResponse;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long amount;

    public Account(Long userId, Long amount) {
        this.userId = userId; this.amount = amount;
    }

    public static Account create(Long userId, Long amount) {
        Account account = new Account();
        account.userId = userId;
        account.amount = amount;
        return account;
    }

    public void charge(Long chargeAmount) {
        this.amount += chargeAmount;
    }

    public void use(Long useAmount) {
        if (this.amount < useAmount) {
            throw new IllegalArgumentException("잔액 부족");
        }
        this.amount -= useAmount;
    }


    public AccountResponse toResponse() {
        return new AccountResponse(userId, amount);
    }


    public AccountHistory toHistory(TransactionType type) {
        return AccountHistory.builder()
                .userId(this.userId)
                .amount(this.amount)
                .type(type)
                .createdDate(LocalDateTime.now())
                .build();
    }
}
