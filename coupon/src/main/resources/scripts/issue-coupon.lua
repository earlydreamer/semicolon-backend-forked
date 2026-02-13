local countKey = KEYS[1]   -- 현재 남은 수량을 저장하는 키 (카운터)
local userSetKey = KEYS[2] -- 발급된 유저 UUID 저장 SET
local limitKey = KEYS[3]   -- (사용 안 함 - 최초 세팅용으로만 활용 권장)
local userUuid = ARGV[1]

-- 1. 중복 발급 체크
if redis.call('SISMEMBER', userSetKey, userUuid) == 1 then
    return -2
end

-- 2. 수량 체크 및 차감 (Atomic 연산)
-- 초기 수량(100)이 countKey에 미리 저장되어 있어야 합니다.
local remaining = redis.call('DECR', countKey)

if remaining < 0 then
    redis.call('INCR', countKey) -- 마이너스가 되면 다시 복구
    return -1 -- 매진
end

-- 3. 유저 등록
redis.call('SADD', userSetKey, userUuid)
return 1 -- 성공