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
@Builder
public class Coupon {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    private LocalDateTime validFrom;

    private LocalDateTime validTo;

    public Coupon(Long id, String name, LocalDateTime validFrom, LocalDateTime validTo) {
        this.id = id;
        this.name = name;
        this.validFrom = validFrom;
        this.validTo = validTo;
    }


    public boolean isActive() {
        LocalDateTime now = LocalDateTime.now();
        return now.isAfter(validFrom) && now.isBefore(validTo);
    }
}
