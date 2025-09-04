# Kafka 기반 선착순(FCFS) 쿠폰 발급 설계

## 1. 목적/배경
- 대규모 트래픽에서도 **선착순 보장**과 **초과/중복 발급 방지**를 달성.
- 요청은 **즉시 접수(대기열/줄세우기)** 하고, **뒤에서 순차 처리**하여 UX 지연을 최소화.

---

## 2. 범위
- 쿠폰 발급 **요청 프로듀서**: `CouponServiceImpl.issueCoupon(...)`
- 쿠폰 발급 **처리 컨슈머**: `CouponIssueKafkaListener.onIssue(...)`
- 주문 완료 **이벤트 발행**: `OrderCompletedKafkaListener` (AFTER_COMMIT)

---

## 3. 토픽/파티션 설계

### 3.1 토픽 명
- **쿠폰 발급 요청**: `coupon-publish-request` (`KafkaProducerConfig.COUPON_REQ_TOPIC`)
- **주문 완료**: `order.completed.v1` (`KafkaProducerConfig.ORDER_TOPIC`)

### 3.2 파티셔닝 전략
- **Key = couponId(Long)** 로 전송 → **동일 쿠폰** 요청은 **같은 파티션**에서 **순서 보장**.
- 장점: 단순하며 FCFS에 적합.  
  유의: 특정 쿠폰이 “핫”하면 해당 파티션이 병목이 될 수 있음(필요 시 파티션 수/토픽 버전업으로 대응).

---

## 4. 메시지 스키마 (요청 Value)
- 타입: `KafkaCouponIssueDTO`
- 필드:
    - `userId: Long`
    - `couponId: Long`

---

## 5. 프로듀서 구성

### 5.1 설정 클래스
`KafkaProducerConfig`
- 공통: `acks=all`, `enable.idempotence=true`, `retries=Integer.MAX_VALUE`, `delivery.timeout.ms=120000`
- 쿠폰 발급 요청 프로듀서:
    - Key 직렬화: `LongSerializer`
    - Value 직렬화: `JsonSerializer` (`ADD_TYPE_INFO_HEADERS=false`)
    - 성능 옵션: `compression=lz4`, `linger.ms=5`, `batch.size=65536`
    - Bean: `@Bean(name="couponIssueKafkaTemplate") KafkaTemplate<Long, KafkaCouponIssueDTO>`

- 주문 완료 프로듀서:
    - Key 직렬화: `StringSerializer`
    - Value 직렬화: `JsonSerializer` (`ADD_TYPE_INFO_HEADERS=false`)
    - Bean: `@Bean(name="orderCompleteKafkaTemplate") KafkaTemplate<String, CompleteMsg>`

### 5.2 요청 전송 코드
`CouponServiceImpl.issueCoupon(Long userId, Long couponId)`
```java
KafkaCouponIssueDTO payload = new KafkaCouponIssueDTO(userId, couponId, System.currentTimeMillis());
couponIssueTemplate.send(KafkaProducerConfig.COUPON_REQ_TOPIC, couponId, payload)
    .whenComplete((res, ex) -> { /* 성공/실패 로깅 */ });
return true; // 의미: "요청을 큐에 넣었음(접수)" — 발급 완료 아님
