package kr.hhplus.be.server.domain.coupon;

import kr.hhplus.be.server.infrastructure.repository.coupon.UserCouponJpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponRepository  {

    Optional<UserCoupon> findByUserIdAndCoupon_Id(Long userId, Long couponId);
    Optional<UserCoupon> findByUserIdAndCouponIdWithLock(Long userId, Long couponId);
    List<UserCoupon> findAllByUserId(Long userId);
    UserCoupon save(UserCoupon userCoupon);
}
