package kr.hhplus.be.server.infrastructure.repository.coupon;

import kr.hhplus.be.server.domain.coupon.Coupon;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CouponJpaRepository extends JpaRepository<Coupon, Long> {

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select c from Coupon c where c.id = :id")
//    Optional<Coupon> findByIdWithPessimisticLock(@Param("id") Long couponId);

//    @Lock(LockModeType.PESSIMISTIC_WRITE)
//    @Query("select c from Coupon c where c.id = :id")
//    Optional<Coupon> findByIdWithLock(@Param("id") Long id);

    @Modifying(clearAutomatically = false, flushAutomatically = true)
    @Query("""
           UPDATE Coupon c
              SET c.quantity = c.quantity - 1
            WHERE c.id = :couponId
              AND c.quantity > 0
              AND c.validFrom < CURRENT_TIMESTAMP
              AND c.validTo   > CURRENT_TIMESTAMP
           """)
    int decreaseIfAvailable(@Param("couponId") Long couponId);

    @Modifying
    @Query("UPDATE Coupon c SET c.quantity = c.quantity + 1 WHERE c.id = :couponId")
    int increase(@Param("couponId") Long couponId);

}


