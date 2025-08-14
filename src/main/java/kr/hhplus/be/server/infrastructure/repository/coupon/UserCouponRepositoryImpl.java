package kr.hhplus.be.server.infrastructure.repository.coupon;

import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

@Repository
@RequiredArgsConstructor
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private UserCouponJpaRepository jpaRepository;
}
