package kr.hhplus.be.server.coupon;


import com.fasterxml.jackson.databind.ObjectMapper;
import kr.hhplus.be.server.controller.coupon.CouponController;
import kr.hhplus.be.server.controller.coupon.request.IssueCouponRequest;
import kr.hhplus.be.server.controller.coupon.request.UseCouponRequest;
import kr.hhplus.be.server.controller.coupon.response.OwnedCouponResponse;
import kr.hhplus.be.server.controller.coupon.response.RegisteredCouponResponse;
import kr.hhplus.be.server.service.coupon.CouponService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(CouponController.class)
class CouponControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private CouponService couponService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void 쿠폰_발급_성공_테스트() throws Exception {
        IssueCouponRequest request = new IssueCouponRequest(1L, 100L);

        given(couponService.issueCoupon(anyLong(), anyLong())).willReturn(true);

        mockMvc.perform(post("/api/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("쿠폰 발급 완료"));
    }

    @Test
    void 쿠폰_발급_실패_테스트() throws Exception {
        IssueCouponRequest request = new IssueCouponRequest(1L, 100L);

        given(couponService.issueCoupon(anyLong(), anyLong())).willReturn(false);

        mockMvc.perform(post("/api/coupons/issue")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("쿠폰 발급 실패: 조건 불충족 또는 수량 소진"));
    }

    @Test
    void 쿠폰_사용_성공_테스트() throws Exception {
        UseCouponRequest request = new UseCouponRequest(1L, 100L);

        given(couponService.useCoupon(anyLong(), anyLong())).willReturn(true);

        mockMvc.perform(post("/api/coupons/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("쿠폰 사용 완료"));
    }

    @Test
    void 쿠폰_사용_실패_테스트() throws Exception {
        UseCouponRequest request = new UseCouponRequest(1L, 100L);

        given(couponService.useCoupon(anyLong(), anyLong())).willReturn(false);

        mockMvc.perform(post("/api/coupons/use")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message").value("쿠폰 사용 실패: 조건 불충족"));
    }

    @Test
    void 유저_쿠폰_목록_조회_테스트() throws Exception {
        List<OwnedCouponResponse> mockList = List.of(
                new OwnedCouponResponse(1L, "쿠폰A", false),
                new OwnedCouponResponse(2L, "쿠폰B", true)
        );

        given(couponService.getOwnedCoupons(anyLong())).willReturn(mockList);

        mockMvc.perform(get("/api/coupons/owned/{userId}", 1L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].couponId").value(1))
                .andExpect(jsonPath("$[0].couponName").value("쿠폰A"))
                .andExpect(jsonPath("$[0].expired").value(false))
                .andExpect(jsonPath("$[1].couponId").value(2))
                .andExpect(jsonPath("$[1].couponName").value("쿠폰B"))
                .andExpect(jsonPath("$[1].expired").value(true));
    }

    @Test
    void 현재_등록_쿠폰_목록_조회_테스트() throws Exception {
        List<RegisteredCouponResponse> mockList = List.of(
                new RegisteredCouponResponse(1L, "등록쿠폰A", true, 10),
                new RegisteredCouponResponse(2L, "등록쿠폰B", false, 0)
        );

        given(couponService.getRegisteredCoupons()).willReturn(mockList);

        mockMvc.perform(get("/api/coupons/registered"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].couponId").value(1))
                .andExpect(jsonPath("$[0].couponName").value("등록쿠폰A"))
                .andExpect(jsonPath("$[0].active").value(true))
                .andExpect(jsonPath("$[0].remainingQuantity").value(10))
                .andExpect(jsonPath("$[1].couponId").value(2))
                .andExpect(jsonPath("$[1].couponName").value("등록쿠폰B"))
                .andExpect(jsonPath("$[1].active").value(false))
                .andExpect(jsonPath("$[1].remainingQuantity").value(0));
    }
}
