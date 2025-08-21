-- 비동기 선착순 쿠폰 발급 (원자 처리)
-- KEYS[1] = coupon hash key (coupon:{couponId})
-- KEYS[2] = issued set key  (coupon:issued:{couponId})
-- KEYS[3] = global issue queue list key (coupon:issue:queue)
-- ARGV[1] = userId
-- ARGV[2] = nowEpochSec (seconds, UTC)
-- ARGV[3] = couponId (queue payload용)

-- Return:
--  >=0 : remaining stock after decrement (queued successfully)
--  -1  : out of stock
--  -2  : duplicate (already issued)
--  -3  : not active time window
--  -4  : coupon hash not loaded (missing)

local couponKey = KEYS[1]
local issuedKey = KEYS[2]
local queueKey  = KEYS[3]

local userId    = ARGV[1]
local now       = tonumber(ARGV[2])
local couponId  = ARGV[3]

-- 1) 쿠폰 해시가 없는 경우 (아직 로딩 안됐거나 만료됨)
if redis.call('EXISTS', couponKey) == 0 then
    return -4
end

-- 2) 기간 체크
local vfrom = tonumber(redis.call('HGET', couponKey, 'valid_from_ts') or '0')
local vto   = tonumber(redis.call('HGET', couponKey, 'valid_to_ts') or '0')
if now < vfrom or now > vto then
    return -3
end

-- 3) 중복 발급 방지
if redis.call('SISMEMBER', issuedKey, userId) == 1 then
    return -2
end

-- 4) 재고 확인/차감
local stock = tonumber(redis.call('HGET', couponKey, 'stock') or '0')
if stock <= 0 then
    return -1
end
local remaining = redis.call('HINCRBY', couponKey, 'stock', -1)

-- 5) 발급 기록(중복 방지 세트에 추가)
redis.call('SADD', issuedKey, userId)

-- 6) 큐에 Job 적재 (비동기 DB persist 용)
local payload = cjson.encode({
    couponId = couponId,
    userId   = userId,
    issuedAt = now
})
redis.call('RPUSH', queueKey, payload)

return remaining
