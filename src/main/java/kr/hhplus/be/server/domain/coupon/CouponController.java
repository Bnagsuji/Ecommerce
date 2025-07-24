package kr.hhplus.be.server.domain.coupon;


import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import kr.hhplus.be.server.domain.coupon.requeset.IssueCouponRequest;
import kr.hhplus.be.server.domain.coupon.requeset.UseCouponRequest;
import kr.hhplus.be.server.domain.coupon.response.IssueCouponResponse;
import kr.hhplus.be.server.domain.coupon.response.OwnedCouponResponse;
import kr.hhplus.be.server.domain.coupon.response.RegisteredCouponResponse;
import kr.hhplus.be.server.domain.coupon.response.UseCouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name="쿠폰",description = "쿠폰 관련 API")
@RestController
@RequestMapping("/api/coupons")
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;

    @PostMapping("/issue")
    @Operation(summary = "쿠폰 발급",description = "쿠폰 발급 하는 API")
    public ResponseEntity<IssueCouponResponse> issueCoupon(@RequestBody IssueCouponRequest request) {
        boolean result = couponService.issueCoupon(request.getMemberId(), request.getCouponId());
        if(result) {
            return ResponseEntity.ok(new IssueCouponResponse(true, "쿠폰 발급 완료"));
        } else {
            return ResponseEntity.badRequest().body(new IssueCouponResponse(false, "쿠폰 발급 실패: 조건 불충족 또는 수량 소진"));
        }
    }

    @PostMapping("/use")
    @Operation(summary = "사용자 쿠폰 사용",description = "사용자 쿠폰 사용 하는 API")
    public ResponseEntity<UseCouponResponse> useCoupon(@RequestBody UseCouponRequest request) {
        boolean result = couponService.useCoupon(request.getMemberId(), request.getCouponId());
        if (result) {
            return ResponseEntity.ok(new UseCouponResponse(true, "쿠폰 사용 완료"));
        } else {
            return ResponseEntity.badRequest().body(new UseCouponResponse(false, "쿠폰 사용 실패: 조건 불충족"));
        }
    }

    @GetMapping("/owned/{memberId}")
    @Operation(summary = "사용자 쿠폰 목록 조회",description = "사용자 쿠폰 목록 조회 하는 API")
    public ResponseEntity<List<OwnedCouponResponse>> getOwnedCoupons(@PathVariable Long memberId) {
        List<OwnedCouponResponse> ownedCoupons = couponService.getOwnedCoupons(memberId);
        return ResponseEntity.ok(ownedCoupons);
    }

    @GetMapping("/registered")
    @Operation(summary = "현재 등록 쿠폰 목록 조회",description = "현재 등록된 쿠폰 목록 조회 하는 API")
    public ResponseEntity<List<RegisteredCouponResponse>> getRegisteredCoupons() {
        List<RegisteredCouponResponse> list = couponService.getRegisteredCoupons();
        return ResponseEntity.ok(list);
    }




}
