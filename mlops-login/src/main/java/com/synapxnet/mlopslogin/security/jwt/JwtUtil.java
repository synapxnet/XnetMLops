package com.synapxnet.mlopslogin.security.jwt;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")  // 从配置文件读取密钥
    private String secretKey;

    @Value("${jwt.expiration}") // 过期时间（毫秒）
    private long expirationTime;

    // 生成 Token（基于用户手机号）
    public String generateToken(String phone) {
        return Jwts.builder()
                .setSubject(phone)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expirationTime))
                .signWith(SignatureAlgorithm.HS256, secretKey)
                .compact();
    }
    // 解析 Token 并提取手机号（即 subject）
    // 新增方法：解析 Token 并提取手机号（subject）
    public String extractUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(secretKey)
                    .parseClaimsJws(token)
                    .getBody();
            return claims.getSubject();  // 返回手机号
        } catch (JwtException | IllegalArgumentException e) {
            throw new JwtException("无效或过期的 Token");
        }
    }
    public Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(secretKey)
                .parseClaimsJws(token)
                .getBody();
    }
}
