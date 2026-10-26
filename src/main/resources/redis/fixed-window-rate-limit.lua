local key = KEYS[1]
local limit = tonumber(ARGV[1])
local windowMillis = tonumber(ARGV[2])

local count = tonumber(redis.call("GET", key) or "0")
local remainingMillis = redis.call("PTTL", key)

if count >= limit then
    if remainingMillis < 0 then
        redis.call("PEXPIRE", key, windowMillis)
        remainingMillis = windowMillis
    end

    return {0, 0, remainingMillis}
end

count = redis.call("INCR", key)

if count == 1 or remainingMillis < 0 then
    redis.call("PEXPIRE", key, windowMillis)
    remainingMillis = windowMillis
end

return {1, limit - count, remainingMillis}