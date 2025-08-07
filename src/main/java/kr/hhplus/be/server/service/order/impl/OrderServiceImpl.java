package kr.hhplus.be.server.service.order.impl;

import kr.hhplus.be.server.controller.account.response.AccountResponse;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.product.request.ProductStockRequest;
import kr.hhplus.be.server.controller.product.response.ProductResponse;
import kr.hhplus.be.server.domain.order.Order;
import kr.hhplus.be.server.domain.order.OrderItem;
import kr.hhplus.be.server.infrastructure.repository.order.OrderJpaRepository;
import kr.hhplus.be.server.service.account.AccountService;
import kr.hhplus.be.server.service.order.OrderService;
import kr.hhplus.be.server.service.product.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class OrderServiceImpl implements OrderService {


    // 주문 아이템 생성
    private List<OrderItem> createOrderItems(OrderRequest req, Map<Long, ProductResponse> productMap) {
        return req.getItems().stream()
                .map(item -> OrderItem.of(item, productMap.get(item.getProductId())))
                .toList();
    }


}
