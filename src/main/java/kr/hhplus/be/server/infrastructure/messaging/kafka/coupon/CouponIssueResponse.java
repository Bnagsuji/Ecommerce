package kr.hhplus.be.server.infrastructure.messaging.kafka.coupon;

public record CouponIssueResponse(
   String reqId,
   Long userId,
   Long couponId,
   long requestedDate
) {}
