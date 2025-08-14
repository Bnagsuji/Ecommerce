package kr.hhplus.be.server.infrastructure.repository.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select c from Coupon c where c.id = :id")
//    Optional<Coupon> findByIdWithPessimisticLock(@Param("id") Long couponId);

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select c from Coupon c where c.id = :id")
//    Optional<Coupon> findByIdWithLock(@Param("id") Long id);



}


