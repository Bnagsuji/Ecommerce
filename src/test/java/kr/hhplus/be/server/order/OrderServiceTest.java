package kr.hhplus.be.server.order;

import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;


@ExtendWith(MockitoExtension.class) // Mockito를 JUnit 5와 함께 사용하기 위한 확장
class OrderServiceTest {

/*    @Mock // Mock 객체로 만들 의존성들
    private AccountServiceImpl accountService;

    @Mock
    private ProductServiceImpl productService;

    @Mock
    private ExternalPlatformServiceImpl externalPlatformServiceImpl;

    @Mock
    private OrderJpaRepository orderRepository;

    @InjectMocks
    private OrderServiceImpl orderService;

    private AtomicLong mockOrderIdCounter;


    @Test
    void 상품재고_차감_요청_정상_테스트() {
        // given
        Long productId = 1L;
        int quantity = 3;

        List<OrderRequest.OrderItem> items = List.of(new OrderRequest.OrderItem(productId, quantity));
        List<ProductResponse> expected = List.of(new ProductResponse(productId, "test", 100, 1000, LocalDateTime.now()));

        when(productService.useProduct(anyList())).thenReturn(expected);

        // when
        List<ProductResponse> result = orderService.deductProductStock(items);

        // then
        assertThat(result).hasSize(1);
        verify(productService).useProduct(anyList());
    }


    @Test
    void 상품재고_부족_시_예외처리_테스트() {
        // given
        Long productId = 1L;
        int requestedQty = 10;
        int currentStock = 5;

        List<OrderRequest.OrderItem> items = List.of(new OrderRequest.OrderItem(productId, requestedQty));

        // 재고가 부족한 응답
        List<ProductResponse> responses = List.of(
                new ProductResponse(productId, "상품", 1000, currentStock, LocalDateTime.now())
        );

        when(productService.useProduct(anyList())).thenThrow(new IllegalArgumentException("재고가 부족합니다."));

        assertThatThrownBy(() -> orderService.deductProductStock(items))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("재고가 부족합니다.");

    }

    @Test
    void 상품리스트_Map으로_변환_테스트() {
        // given
        ProductResponse res1 = new ProductResponse(1L, "상품A", 1000, 10, LocalDateTime.now());
        ProductResponse res2 = new ProductResponse(2L, "상품B", 2000, 20, LocalDateTime.now());

        List<ProductResponse> input = List.of(res1, res2);

        // when
        Map<Long, ProductResponse> result = orderService.mapProductResponses(input);

        // then
        assertThat(result).hasSize(2);
        assertThat(result.get(1L)).isEqualTo(res1);
    }

    @Test
    void OrderItem_생성_테스트() {
        // given
        Long productId = 1L;
        int quantity = 2;
        ProductResponse product = new ProductResponse(productId, "상품", 100, 99, LocalDateTime.now());

        List<OrderRequest.OrderItem> requestItems = List.of(new OrderRequest.OrderItem(productId, quantity));
        Map<Long, ProductResponse> productMap = Map.of(productId, product);

        // when
        List<OrderItem> result = orderService.createOrderItems(requestItems, productMap);

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getQuantity()).isEqualTo(2);
        assertThat(result.get(0).getId()).isEqualTo(productId);
    }

    @Test
    void 상품_가격_0이하_시_예외처리_테스트() {
        // given
        Long productId = 1L;
        int quantity = 2;

        ProductResponse product = new ProductResponse(productId, "상품", 0, 100, LocalDateTime.now());

        List<OrderRequest.OrderItem> items = List.of(new OrderRequest.OrderItem(productId, quantity));
        Map<Long, ProductResponse> productMap = Map.of(productId, product);

        // when & then
        assertThatThrownBy(() -> orderService.createOrderItems(items, productMap))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품 금액이 0 이하입니다.");
    }

    @Test
    void 주문요청에_아이템이_없을_시_예외처리_테스트() {
        // given
        OrderRequest request = new OrderRequest(1L, null, List.of());

        // when & then
        assertThatThrownBy(() -> orderService.placeOrder(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("상품이 존재하지 않습니다.");
    }

    @Test
    void OrderItem목록으로부터_총금액을_계산_테스트() {
        // given
        ProductResponse product = new ProductResponse(1L, "상품", 2000, 99, LocalDateTime.now());
        OrderItem item1 = OrderItem.of(product, 2); // 2000 * 2 = 4000
        OrderItem item2 = OrderItem.of(product, 1); // 2000 * 1 = 2000

        List<OrderItem> items = List.of(item1, item2);

        // when
        long result = 6000L;

        // then
        assertThat(result).isEqualTo(6000L);
    }

    @Test
    void 계좌_잔액_차감_테스트() {
        // given
        Long userId = 1L;
        long amount = 5000L;

        when(accountService.useBalance(userId, amount))
                .thenReturn(new AccountResponse(userId, 15000L));

        // when
        AccountResponse result = orderService.deductBalance(userId, amount);

        // then
        assertThat(result.amount()).isEqualTo(15000L);
    }

    @Test
    void 잔액_부족_시_예외처리_테스트() {
        // given
        Long userId = 1L;
        long totalAmount = 10000L;

        // accountService가 잔액 부족 처리 시 null이나 예외를 반환한다고 가정
        when(accountService.useBalance(userId, totalAmount)).thenThrow(
                new IllegalStateException("잔액 부족")
        );

        // when & then
        assertThatThrownBy(() -> orderService.deductBalance(userId, totalAmount))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("잔액 부족");
    }

    @Test
    void 주문_생성_테스트() {
        // given
        OrderRequest request = new OrderRequest(1L, null, List.of(
                new OrderRequest.OrderItem(1L, 2)
        ));
        long amount = 5000L;

        ProductResponse product = new ProductResponse(1L, "상품", 2500, 100, LocalDateTime.now());
        OrderItem item = OrderItem.of(product, 2);

        // when
        Order order = orderService.createOrder(request, amount, List.of(item));

        // then
        assertThat(order.getTotalAmount()).isEqualTo(amount);
        assertThat(order.getOrderItems()).hasSize(1);
    }


    @Test
    void 주문엔티티_잔액_응답객체_변환_테스트() {
        // given
        Order order = Order.create(
                new OrderRequest(1L, null, List.of(
                        new OrderRequest.OrderItem(1L, 2)
                )),
                10000L,
                List.of(OrderItem.of(new ProductResponse(1L, "상품", 5000, 100, LocalDateTime.now()), 2))
        );
        // when
        OrderResponse response = orderService.buildOrderResponse(order);

        // then
        assertThat(response.orderId()).isEqualTo(1L);
    }*/
}
