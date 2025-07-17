package kr.hhplus.be.server.domain.account.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class AccountHistoryResponse {
        String type; // "CHARGE" or "USE"z
        int amount;
        LocalDateTime dateTime;
}
