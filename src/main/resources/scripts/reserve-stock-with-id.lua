
-- KEYS[1] = stock key
-- KEYS[2] = reservation key
-- ARGV[1] = product ID
-- ARGV[2] = reservation ID

-- Check whether this reservation already exists.
local existingProduct = redis.call('GET', KEYS[2])

if existingProduct then
    if existingProduct == ARGV[1] then
        return 2
    else
        return -2
    end
end

-- Read and validate stock.
local stockValue = redis.call('GET', KEYS[1])

if not stockValue then
    return -1
end

local stock = tonumber(stockValue)

if not stock or stock < 0 or stock ~= math.floor(stock) then
    return -3
end

if stock == 0 then
    return 0
end

-- Atomically decrement stock and record the reservation.
redis.call('DECR', KEYS[1])
redis.call('SET', KEYS[2], ARGV[1])

return 1