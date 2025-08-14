package kr.hhplus.be.server.domain.coupon;

import java.util.List;
import java.util.Optional;

public interface CouponRepository {
//    Optional<Coupon> findByIdWithPessimisticLock(Long couponId);
    Optional<Coupon> findById(Long couponId);
    List<Coupon> findAll();
    Coupon save(Coupon coupon);
    void deleteAll();
}
