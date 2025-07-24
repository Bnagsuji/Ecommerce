package kr.hhplus.be.server.controller.account.request;

import lombok.*;

@Setter
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class AccountRequest {
    private Long userId;
    private Long amount;
}


