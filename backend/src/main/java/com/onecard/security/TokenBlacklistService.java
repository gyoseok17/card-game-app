package com.onecard.security;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final StringRedisTemplate redisTemplate;

    public void blacklist(String token, long remainingMs) {
        if (remainingMs > 0) {
            redisTemplate.opsForValue().set("blacklist:" + token, "1", remainingMs, TimeUnit.MILLISECONDS);
        }
    }

    public boolean isBlacklisted(String token) {
        return Boolean.TRUE.equals(redisTemplate.hasKey("blacklist:" + token));
    }

    public void saveCurrentToken(Long userId, String token, long expirationMs) {
        redisTemplate.opsForValue().set("current_token:" + userId, token, expirationMs, TimeUnit.MILLISECONDS);
    }

    public String getCurrentToken(Long userId) {
        return redisTemplate.opsForValue().get("current_token:" + userId);
    }

    public void removeCurrentToken(Long userId) {
        redisTemplate.delete("current_token:" + userId);
    }
}
