-- Seck8s 秒杀库存扣减脚本：一次原子完成「限购校验 + 库存扣减」，杜绝超卖
-- KEYS[1] = seck8s:stock:{skuId}          剩余库存
-- KEYS[2] = seck8s:bought:{activityId}:{userId}  该用户已购数
-- ARGV[1] = quantity         购买数量
-- ARGV[2] = quotaPerUser     每用户限购数
-- ARGV[3] = expireSeconds    bought key 的 TTL（活动剩余时长）
-- 返回：1 成功 / -1 未预热或活动未开始 / -2 售罄 / -3 超限购
local qty   = tonumber(ARGV[1])
local limit = tonumber(ARGV[2])
if qty == nil or qty < 1 then
    return -1
end

local stock = tonumber(redis.call('GET', KEYS[1]) or '-1')
if stock < 0 then
    return -1   -- 未预热（库存 key 不存在）
end
if stock < qty then
    return -2   -- 售罄
end

local bought = tonumber(redis.call('GET', KEYS[2]) or '0')
if bought + qty > limit then
    return -3   -- 超限购
end

redis.call('DECRBY', KEYS[1], qty)
redis.call('INCRBY', KEYS[2], qty)
if redis.call('TTL', KEYS[2]) < 0 then
    redis.call('EXPIRE', KEYS[2], tonumber(ARGV[3]))
end
return 1
