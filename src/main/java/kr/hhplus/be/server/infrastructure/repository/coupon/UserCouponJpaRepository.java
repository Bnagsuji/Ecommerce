package kr.hhplus.be.server.infrastructure.repository.coupon;

import jakarta.persistence.LockModeType;
import kr.hhplus.be.server.domain.coupon.UserCoupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserCouponJpaRepository extends JpaRepository<UserCoupon, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT uc FROM UserCoupon uc WHERE uc.userId = :userId AND uc.coupon.id = :couponId")
    Optional<UserCoupon> findByUserIdAndCouponIdWithLock(@Param("userId") Long userId, @Param("couponId") Long couponId);

    //멱등체크
    boolean existsByUserIdAndCoupon_Id(Long userId, Long couponId);

    Optional<UserCoupon> findByUserIdAndCoupon_Id(Long userId, Long couponId);
    List<UserCoupon> findAllByUserId(Long userId);
}
