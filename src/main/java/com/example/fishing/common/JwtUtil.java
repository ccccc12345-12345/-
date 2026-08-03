package com.example.fishing.common;

import cn.hutool.core.date.DateUtil;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import java.util.Date;

/**
 * 简易 JWT 工具类
 */
@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretProp;

    @Value("${jwt.expiration}")
    private Long expirationProp;

    private static String secret;
    private static Long expiration;

    @PostConstruct
    public void init() {
        secret = secretProp;
        expiration = expirationProp;
    }

    public static String generateToken(Long userId) {
        Date now = new Date();
        Date expire = DateUtil.offsetMillisecond(now, expiration.intValue());
        return Jwts.builder()
                .setSubject(String.valueOf(userId))
                .setIssuedAt(now)
                .setExpiration(expire)
                .signWith(SignatureAlgorithm.HS256, secret)
                .compact();
    }

    public static Long getUserId(String token) {
        Claims claims = parseToken(token);
        return Long.valueOf(claims.getSubject());
    }

    public static Claims parseToken(String token) {
        return Jwts.parser()
                .setSigningKey(secret)
                .parseClaimsJws(token)
                .getBody();
    }

    public static boolean validate(String token) {
        try {
            Claims claims = parseToken(token);
            return claims.getExpiration().after(new Date());
        } catch (Exception e) {
            return false;
        }
    }
}
