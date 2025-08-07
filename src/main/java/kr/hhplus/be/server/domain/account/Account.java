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
@Builder(access = AccessLevel.PRIVATE)
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long amount;

    public Account(Long userId, Long amount) {
        this.userId = userId; this.amount = amount;
    }


    //새 계좌 생성
    public static Account createNew(Long userId) {
        return Account.builder()
                .userId(userId)
                .amount(0L)
                .build();
    }



    public static Account create(Long userId, Long amount) {
        return Account.builder()
                .userId(userId)
                .amount(amount)
                .build();
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


}
