
-- KEYS[1] = stock key
-- KEYS[2] = reservation key
-- ARGV[1] = product ID
-- ARGV[2] = reservation ID

-- If this reservation exists, verify it belongs to this product.
local existingProduct = redis.call('GET', KEYS[2])

if existingProduct then
    if existingProduct == ARGV[1] then
        return 2
    else
        return -2
    end
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