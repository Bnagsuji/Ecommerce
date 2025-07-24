package kr.hhplus.be.server.controller.order;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.service.order.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "주문",description = "주문 관련 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    @Operation(summary = "주문 요청", description = "상품 목록과 수량, 선택 쿠폰을 포함해 주문을 처리 하는 API.")
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest request) {
        OrderResponse response = orderService.placeOrder(request);
        return ResponseEntity.ok(response);
    }
}

