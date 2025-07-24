package kr.hhplus.be.server.domain.order.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class OrderResponse {
    private boolean success;
    private String message;
}