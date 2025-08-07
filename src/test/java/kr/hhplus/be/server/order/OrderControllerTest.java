package kr.hhplus.be.server.order;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.controller.order.OrderController;
import kr.hhplus.be.server.controller.order.request.OrderRequest;
import kr.hhplus.be.server.controller.order.request.OrderRequest.OrderItem;
import kr.hhplus.be.server.controller.order.response.OrderResponse;
import kr.hhplus.be.server.controller.order.response.OrderResponse.OrderItemResponse;
import kr.hhplus.be.server.service.order.OrderService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(OrderController.class)
class OrderControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private OrderService orderService;

    @Autowired
    private ObjectMapper objectMapper;
/*
    @Test
    void 주문_생성_성공_테스트() throws Exception {
        // given
        OrderRequest request = new OrderRequest(
                1L,
                10L,
                List.of(
                        new OrderItem(100L, 2),
                        new OrderItem(101L, 1)
                )
        );

        OrderResponse mockResponse = new OrderResponse(
                123L,
                1L,
                12000L,
                "2025-07-24T13:00:00",
                "COMPLETED",
                List.of(
                        new OrderItemResponse(100L, "상품A", 5000L, 2, 10000L),
                        new OrderItemResponse(101L, "상품B", 2000L, 1, 2000L)
                )
        );

        given(orderService.placeOrder(any(OrderRequest.class))).willReturn(mockResponse);

        // when & then
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orderId").value(123))
                .andExpect(jsonPath("$.userId").value(1))
                .andExpect(jsonPath("$.totalAmount").value(12000))
                .andExpect(jsonPath("$.orderDate").value("2025-07-24T13:00:00"))
                .andExpect(jsonPath("$.status").value("COMPLETED"));
    }*/
}

