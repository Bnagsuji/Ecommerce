package kr.hhplus.be.server.domain.account.request;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Setter
@Getter
@NoArgsConstructor
public class AccountRequest {
    private Long memberId;
    private int amount;
}


