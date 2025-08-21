package kr.hhplus.be.server.lock.key;

public final class RedisKeys {
    private RedisKeys() {}

    public static String couponHash(long couponId) {
        return "coupon:" + couponId;
    }

    public static String issuedSet(long couponId) {
        return "coupon:issued:" + couponId;
    }

    public static String issueQueue() {
        return "coupon:issue:queue";
    }

    public static String issueDlq() {
        return "coupon:issue:dlq";
    }
}
