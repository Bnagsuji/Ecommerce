package kr.hhplus.be.server.infrastructure.repository.coupon;

import kr.hhplus.be.server.domain.coupon.UserCoupon;
import kr.hhplus.be.server.domain.coupon.UserCouponRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Example;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.repository.query.FluentQuery;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@Repository
@RequiredArgsConstructor
public class UserCouponRepositoryImpl implements UserCouponRepository {

    private final UserCouponJpaRepository userCouponJpaRepository;

    @Override
    public Optional<UserCoupon> findByUserIdAndCouponIdWithLock(Long userId, Long couponId) {
        return userCouponJpaRepository.findByUserIdAndCouponIdWithLock(userId, couponId);
    }



    @Override
    public Optional<UserCoupon> findByUserIdAndCoupon_Id(Long userId, Long couponId) {
        return userCouponJpaRepository.findByUserIdAndCoupon_Id(userId, couponId);
    }

    @Override
    public List<UserCoupon> findAllByUserId(Long userId) {
        return userCouponJpaRepository.findAllByUserId(userId);
    }

    @Override
    public UserCoupon save(UserCoupon userCoupon) {
        return userCouponJpaRepository.save(userCoupon);
    }

    @Override
    public void deleteAll() {
        userCouponJpaRepository.deleteAll();
    }
}
