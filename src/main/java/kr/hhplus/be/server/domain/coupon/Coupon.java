package kr.hhplus.be.server.domain.coupon;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    private Integer quantity; // 남은 수량
    private Integer discountAmount;

    @Builder
    public Coupon( String name, LocalDateTime validFrom, LocalDateTime validTo, Integer quantity, Integer discountAmount) {
        this.name = name;
        this.validFrom = validFrom;
        this.validTo = validTo;
        this.quantity = quantity;
        this.discountAmount = discountAmount;
    }

    // 정적 팩토리 메서드
    public static Coupon create(String name, LocalDateTime validFrom, LocalDateTime validTo,Integer quantity, Integer discountAmount) {
        return new Coupon(name, validFrom, validTo, quantity, discountAmount);
    }

    //유효기간 체크
    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(validFrom) && now.isBefore(validTo);
    }


    //쿠폰 재고 감소
    public void decreaseQuantity() {
        if (quantity > 0) this.quantity--;
    }

    public void increaseQuantity() {
        this.quantity++;
    }



    //테스트용
    public void resetQuantity(int quantity) {
        if (quantity < 0) {
            throw new IllegalArgumentException("수량은 0 이상이어야 합니다.");
        }
        this.quantity = quantity;
    }
}
