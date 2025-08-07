package kr.hhplus.be.server.service.coupon.impl;

import kr.hhplus.be.server.controller.coupon.response.OwnedCouponResponse;
import kr.hhplus.be.server.controller.coupon.response.RegisteredCouponResponse;
import kr.hhplus.be.server.domain.coupon.Coupon;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.infrastructure.repository.coupon.CouponJpaRepository;
import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;
import kr.hhplus.be.server.service.coupon.CouponService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@RequiredArgsConstructor
public class CouponServiceImpl implements CouponService {

    private final CouponJpaRepository couponRepository;
    private final UserCouponJpaRepository userCouponRepository;

    @Override
    public boolean issueCoupon(Long userId, Long couponId) {
        Coupon coupon = couponRepository.findById(couponId)
                .filter(Coupon::isActive)
                .orElse(null);
        if (coupon == null) return false;

        // 이미 보유 여부 체크
        if (userCouponRepository.existsByUserIdAndCoupon_Id(userId, couponId)) return false;

        if (coupon.getQuantity() <= 0) return false;

        coupon.decreaseQuantity(); // 재고 차감
        couponRepository.save(coupon);

        UserCoupon userCoupon = UserCoupon.builder()
                .userId(userId)
                .coupon(coupon)
                .build();
        userCouponRepository.save(userCoupon);

        return true;
    }

    @Override
    public List<OwnedCouponResponse> getOwnedCoupons(Long userId) {
        return userCouponRepository.findAllByUserId(userId).stream()
                .map(uc -> OwnedCouponResponse.from(uc.getCoupon()))
                .toList();
    }

    @Override
    public List<RegisteredCouponResponse> getRegisteredCoupons() {
        return couponRepository.findAll().stream()
                .map(coupon -> RegisteredCouponResponse.from(coupon, coupon.getQuantity()))
                .toList();
    }

    @Override
    public boolean useCoupon(Long userId, Long couponId) {
        Optional<UserCoupon> optional = userCouponRepository.findByUserIdAndCoupon_Id(userId, couponId);
        if (optional.isEmpty()) return false;

        UserCoupon userCoupon = optional.get();
        if (userCoupon.isUsed()) return false;

        userCoupon.markUsed();
        userCouponRepository.save(userCoupon);
        return true;
    }

    @Override
    public int applyCoupon(Long userId, Long couponId) {
        return couponRepository.findById(couponId)
                .map(Coupon::getDiscountAmount)
                .orElse(0);
    }

    @Override
    public void rollback(Long userId, Long couponId) {
        userCouponRepository.findByUserIdAndCoupon_Id(userId, couponId)
                .ifPresent(userCoupon -> {
                    userCoupon.rollback();
                    userCouponRepository.save(userCoupon);
                });
    }
}

