package com.example.miniproject.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.security.KeyPair;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class JwtService {

    private final KeyPair keyPair;

    @Qualifier("jwtRedisTemplate") // 使用@Qualifier指定要注入的bean
    private final ReactiveRedisTemplate<String, String> redisTemplate;

    private final Long jwtExpiration;

    private static final String TOKEN_BLACKLIST_PREFIX = "token:blacklist:";
    private static final String TOKEN_CACHE_PREFIX = "token:valid:";

    public String generateToken(String username, List<String> roles) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + jwtExpiration);

        String token = Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(keyPair.getPrivate())
                .compact();

        // 缓存有效令牌
        redisTemplate.opsForValue()
                .set(TOKEN_CACHE_PREFIX + token, "valid", Duration.ofMillis(jwtExpiration))
                .subscribe();

        return token;
    }

    public Mono<Claims> validateToken(String token) {
        // 首先检查令牌是否被列入黑名单
        return redisTemplate.hasKey(TOKEN_BLACKLIST_PREFIX + token)
                .flatMap(isBlacklisted -> {
                    if (isBlacklisted) {
                        return Mono.error(new RuntimeException("Token is blacklisted"));
                    }

                    // 检查令牌是否在缓存中
                    return redisTemplate.opsForValue().get(TOKEN_CACHE_PREFIX + token)
                            .flatMap(cached -> {
                                if (cached != null) {
                                    return parseToken(token);
                                }

                                // 如果不在缓存中，验证并缓存（如果有效）
                                return parseToken(token)
                                        .doOnSuccess(claims ->
                                                redisTemplate.opsForValue()
                                                        .set(TOKEN_CACHE_PREFIX + token,
                                                                "valid",
                                                                Duration.ofMillis(jwtExpiration))
                                                        .subscribe()
                                        );
                            });
                });
    }

    private Mono<Claims> parseToken(String token) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(keyPair.getPublic())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
            return Mono.just(claims);
        } catch (Exception e) {
            return Mono.error(e);
        }
    }

    public void blacklistToken(String token) {
        // 将令牌添加到黑名单，保持与原令牌相同的过期时间
        redisTemplate.opsForValue()
                .set(TOKEN_BLACKLIST_PREFIX + token,
                        "blacklisted",
                        Duration.ofMillis(jwtExpiration))
                .subscribe();

        // 如果存在，则从有效缓存中删除
        redisTemplate.delete(TOKEN_CACHE_PREFIX + token).subscribe();
    }

    public List<GrantedAuthority> getAuthorities(Claims claims) {
        @SuppressWarnings("unchecked")
        List<String> roles = claims.get("roles", List.class);
        return roles.stream()
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    // 为特定用户和角色生成带有自定义过期时间的令牌
    public String generateTokenWithCustomExpiration(String username, List<String> roles, long customExpirationMs) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + customExpirationMs);

        String token = Jwts.builder()
                .setSubject(username)
                .claim("roles", roles)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(keyPair.getPrivate())
                .compact();

        // 缓存有效令牌，使用自定义过期时间
        redisTemplate.opsForValue()
                .set(TOKEN_CACHE_PREFIX + token, "valid", Duration.ofMillis(customExpirationMs))
                .subscribe();

        return token;
    }
}