package kr.hhplus.be.server.domain.account;

import jakarta.persistence.*;
import kr.hhplus.be.server.controller.account.request.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Entity
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long userId;

    private Long amount;

    @Enumerated(EnumType.STRING)
    private TransactionType type;

    private LocalDateTime createdDate;

    public static AccountHistory create(Long userId, Long amount, TransactionType type, LocalDateTime now) {
        return AccountHistory.builder()
                .userId(userId)
                .amount(amount)
                .type(type)
                .createdDate(LocalDateTime.now())
                .build();
    }
}
