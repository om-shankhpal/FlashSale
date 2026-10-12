-- KEYS[1] = stock key
-- KEYS[2] = reservation key
-- ARGV[1] = reservation ID

-- A repeated reservation must not decrement stock again.
if redis.call('EXISTS', KEYS[2]) == 1 then
    return 2
end

local stock = tonumber(redis.call('GET', KEYS[1]))

if not stock then
    return -1
end

if stock <= 0 then
    return 0
end

redis.call('DECR', KEYS[1])
redis.call('SET', KEYS[2], ARGV[1])

return 1