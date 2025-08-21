package kr.hhplus.be.server.controller.coupon.response;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@AllArgsConstructor
public class IssueCouponResponse {

    private boolean success;
    private String message;

    private Long couponId;        // 어떤 쿠폰
    private Long userId;          // 어떤 사용자
    private String status;        // ACCEPTED | DUPLICATE | SOLD_OUT | NOT_ACTIVE | NOT_FOUND
    private Long remainingStock;  // 남은 재고 (성공 시 의미)
    private String reason;        // 상세 사유

    public IssueCouponResponse(){}


    /** 레거시 컨트롤러 호환 생성자 */
    public IssueCouponResponse(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

//    public IssueCouponResponse(boolean success, String message,
//                               Long couponId, Long userId, String status,
//                               Long remainingStock, String reason) {
//        this.success = success;
//        this.message = message;
//        this.couponId = couponId;
//        this.userId = userId;
//        this.status = status;
//        this.remainingStock = remainingStock;
//        this.reason = reason;
//    }


    /** 확장: 성공(큐 적재 완료) 응답을 success/message 스타일로 변환이 필요할 때 사용 가능 */
    public static IssueCouponResponse accepted(Long couponId, Long userId, long remaining) {
        IssueCouponResponse r = new IssueCouponResponse(true, "쿠폰 발급 접수 완료", null, null, null, null, null);
        r.couponId = couponId;
        r.userId = userId;
        r.status = "ACCEPTED";
        r.remainingStock = remaining;
        return r;
    }

    public static IssueCouponResponse duplicate(Long couponId, Long userId) {
        IssueCouponResponse r = new IssueCouponResponse(false, "이미 발급된 사용자입니다.", null, null, null, null, null);
        r.couponId = couponId;
        r.userId = userId;
        r.status = "DUPLICATE";
        r.reason = "이미 발급된 사용자입니다.";
        return r;
    }

    public static IssueCouponResponse soldOut(Long couponId, Long userId) {
        IssueCouponResponse r = new IssueCouponResponse(false, "쿠폰 재고가 소진되었습니다.", null, null, null, null, null);
        r.couponId = couponId;
        r.userId = userId;
        r.status = "SOLD_OUT";
        r.remainingStock = 0L;
        r.reason = "쿠폰 재고가 소진되었습니다.";
        return r;
    }

    public static IssueCouponResponse notActive(Long couponId, Long userId) {
        IssueCouponResponse r = new IssueCouponResponse(false, "쿠폰 유효기간이 아닙니다.", null, null, null, null, null);
        r.couponId = couponId;
        r.userId = userId;
        r.status = "NOT_ACTIVE";
        r.reason = "쿠폰 유효기간이 아닙니다.";
        return r;
    }

    public static IssueCouponResponse notFound(Long couponId, Long userId) {
        IssueCouponResponse r = new IssueCouponResponse(false, "쿠폰 정보가 없습니다.", null, null, null, null, null);
        r.couponId = couponId;
        r.userId = userId;
        r.status = "NOT_FOUND";
        r.reason = "쿠폰 정보가 없습니다.";
        return r;
    }
}