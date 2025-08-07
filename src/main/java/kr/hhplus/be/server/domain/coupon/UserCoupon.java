package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(
        name = "user_coupon",
        uniqueConstraints = @UniqueConstraint(columnNames = {"userId", "coupon_id"})
)
public class UserCoupon {

    //유저가 가진 쿠폰 하나 의미
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @ManyToOne(fetch = FetchType.LAZY)
    private Coupon coupon;

    @Column(nullable = false)
    private boolean used;

    @Builder
    public UserCoupon(Long userId, Coupon coupon) {
        this.userId = userId;
        this.coupon = coupon;
        this.used = false;
    }

    public void markUsed() {
        this.used = true;
    }


    public void rollback() {
        this.used = false;
    }
}

