-- issue-coupon.lua
local countKey = KEYS[1]
local userSetKey = KEYS[2]
local limitKey = KEYS[3]
local userUuid = ARGV[1]

-- 1. 수량 제한 정보 확인
local limit = redis.call('get', limitKey)
if not limit then
    return -3
end

-- 2. 중복 발급 체크
if redis.call('SISMEMBER', userSetKey, userUuid) == 1 then
    return -2
end

-- 3. 현재 발급 수량 체크
local currentCount = redis.call('get', countKey) or "0"
if tonumber(currentCount) >= tonumber(limit) then
    return -1
end

-- 4. 발급 처리
redis.call('incr', countKey)
redis.call('SADD', userSetKey, userUuid)
return 1