package kr.hhplus.be.server.infrastructure.repository.coupon;

import kr.hhplus.be.server.domain.coupon.CouponRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@AllArgsConstructor
public class CouponRepositoryImpl implements CouponRepository {
    private final CouponJpaRepository couponJpaRepository;


}
