package org.example.smashhub.service;


import lombok.AccessLevel;

import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.concurrent.TimeUnit;
@RequiredArgsConstructor
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class TokenBlacklistService {
    public final RedisTemplate<String, String> redisTemplate;
    public static  final String PREFIX = "token:blacklist:";

    public void blacklist(String jti, Date expiryTime){
        long ttlMillis = expiryTime.getTime() - System.currentTimeMillis();
        if (ttlMillis >0){
            redisTemplate.opsForValue()
                    .set(PREFIX + jti,"1", ttlMillis, TimeUnit.MILLISECONDS);
        }
    }
    public boolean isBlacklisted(String jti){
        return Boolean.TRUE.equals(redisTemplate.hasKey(PREFIX+jti));
    }
}
