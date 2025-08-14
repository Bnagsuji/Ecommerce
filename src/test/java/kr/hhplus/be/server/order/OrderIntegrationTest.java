package kr.hhplus.be.server.order;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.request.OrderRequest.OrderItem;
import kr.hhplus.be.server.domain.account.Account;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.product.Product;
import kr.hhplus.be.server.infrastructure.repository.account.AccountJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.product.ProductJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.order.OrderJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Rollback
class OrderIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductJpaRepository productRepo;

    @Autowired
    private AccountJpaRepository accountRepo;

    @Autowired
    private OrderJpaRepository orderRepo;

    @Autowired
    private CouponJpaRepository couponRepository;
    @Autowired private
    UserCouponJpaRepository userCouponRepository;

    private Long userId;

/*
    @BeforeEach
    void setup() {
        userId = 1L;

        // 테스트용 상품 생성
        Product product = new Product("맥북", 200, 10L, LocalDateTime.now());
        productRepo.save(product);


        // 쿠폰 생성 (할인 50,000원)
        Coupon coupon = Coupon.create(
                "할인쿠폰",
                LocalDateTime.now().minusDays(1),
                LocalDateTime.now().plusDays(1),
                1,
                50_000
        );
        couponRepository.save(coupon);

        // 테스트용 계정 생성
        Account account = new Account(userId, 3_000_000L);
        accountRepo.save(account);


        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
        userCouponRepository.save(userCoupon);
    }

    @Test
    void 주문_성공_통합테스트() throws Exception {
        Product product = productRepo.findAll().get(0);
        Coupon coupon = couponRepository.findAll().get(0); // 생성된 쿠폰
        UserCoupon userCoupon = userCouponRepository.findAll().get(0); // 발급된 쿠폰

        OrderRequest request = new OrderRequest(
                userId,
                coupon.getId(),
                List.of(new OrderItem(product.getId(), 1)) // 상품 1개 주문
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").exists())
                .andExpect(jsonPath("$.userId").value(userId))
                .andExpect(jsonPath("$.totalAmount").value(product.getPrice()));

        // 잔액 차감 검증
        Account updated = accountRepo.findById(userId).orElseThrow();
        assertThat(updated.getAmount()).isEqualTo(3_000_000L - product.getPrice());

        /// 쿠폰 수량 감소 확인
        Coupon updatedCoupon = couponRepository.findById(coupon.getId()).orElseThrow();
        assertThat(updatedCoupon.getQuantity()).isEqualTo(0);


        // 유저 쿠폰 사용 여부 확인
        UserCoupon usedCoupon = userCouponRepository.findById(userCoupon.getId()).orElseThrow();
        assertThat(usedCoupon.isUsed()).isTrue();


        // 주문 저장 검증
        assertThat(orderRepo.findAll()).hasSize(1);
    }

    @Test
    void 빈_주문목록_주문_실패() throws Exception {
        OrderRequest request = new OrderRequest(
                userId,
                null,
                List.of() // 빈 리스트
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }

    @Test
    void 없는_상품_주문_실패() throws Exception {
        OrderRequest request = new OrderRequest(
                userId,
                null,
                List.of(new OrderItem(9999L, 1))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().is4xxClientError());
    }


    @Test
    void 재고_부족시_주문_실패() throws Exception {
        Product product = productRepo.findAll().get(0);

        OrderRequest request = new OrderRequest(
                userId,
                null,
                List.of(new OrderItem(product.getId(), 1000)) // 재고 초과
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 잔액_부족시_주문_실패() throws Exception {
        Product product = productRepo.findAll().get(0);

        Account poorAccount = new Account(userId + 1, 10L);
        accountRepo.save(poorAccount);

        OrderRequest request = new OrderRequest(
                poorAccount.getUserId(),
                null,
                List.of(new OrderItem(product.getId(), 1))
        );

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
    }
*/




}
