package kr.hhplus.be.server.infrastructure.messaging.kafka.coupon;

public record KafkaCouponIssueDTO(
        Long userId,
        Long couponId,
        long requestedDate
) {}
