package kr.hhplus.be.server.infrastructure.repository.coupon;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {
    boolean existsByUserIdAndCoupon_Id(Long userId, Long couponId);
    Optional<UserCoupon> findByUserIdAndCoupon_Id(Long userId, Long couponId);
    List<UserCoupon> findAllByUserId(Long userId);
}
