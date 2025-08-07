package kr.hhplus.be.server.controller.product.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class TopSellingProductResponse {
    private Long productId;
    private String productName;
    private Long quantitySold;  // sum 결과라 Long으로 받는 게 맞음
}