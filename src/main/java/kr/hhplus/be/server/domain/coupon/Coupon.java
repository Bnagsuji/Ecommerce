package kr.hhplus.be.server.domain.coupon;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
public class Coupon {
    private Long id;
    private String name;
    private LocalDateTime validFrom;
    private LocalDateTime validTo;
    private int totalQuantity;

    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return (now.isAfter(validFrom) || now.isEqual(validFrom)) && now.isBefore(validTo);
    }
}
