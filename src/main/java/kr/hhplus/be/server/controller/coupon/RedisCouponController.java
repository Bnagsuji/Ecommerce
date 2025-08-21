package kr.hhplus.be.server.controller.coupon;

import jakarta.validation.Valid;
import kr.hhplus.be.server.controller.coupon.request.IssueCouponRequest;
import kr.hhplus.be.server.controller.coupon.response.IssueCouponResponse;
import kr.hhplus.be.server.service.coupon.RedisCouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redisCp")
@RequiredArgsConstructor
public class RedisCouponController {

    private final RedisCouponService redisCouponService;

    @PostMapping("/{couponId}/issue-async")
    public IssueCouponResponse issueAsync(@PathVariable Long couponId,
                                          @Valid @RequestBody IssueCouponRequest req) {
        return redisCouponService.issueAsync(couponId, req.getUserId());
    }
}
