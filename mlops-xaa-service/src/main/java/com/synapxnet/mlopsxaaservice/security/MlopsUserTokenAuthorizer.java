package com.synapxnet.mlopsxaaservice.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 校验 XnetMLOps 登录 JWT 并解析当前用户主体。
 */
@Service
public class MlopsUserTokenAuthorizer {
    private final String jwtSecret;

    /**
     * 创建 MLOps 用户令牌授权器。
     *
     * @param jwtSecret 与登录服务一致的 JWT 密钥
     */
    public MlopsUserTokenAuthorizer(@Value("${mlops.jwt-secret:}") String jwtSecret) {
        this.jwtSecret = jwtSecret == null ? "" : jwtSecret.trim();
    }

    /**
     * 验证 Bearer 令牌并返回手机号主体。
     *
     * @param authorization Bearer 认证头
     * @return 已签名且未过期的用户主体
     */
    public String authorize(String authorization) {
        if (jwtSecret.isBlank()) {
            throw new IllegalStateException("MLOps JWT 密钥未配置");
        }
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            throw new IllegalArgumentException("MLOps 登录令牌无效或已过期");
        }
        String token = authorization.substring("Bearer ".length()).trim();
        try {
            Claims claims = Jwts.parser()
                    .setSigningKey(jwtSecret)
                    .parseClaimsJws(token)
                    .getBody();
            String subject = claims.getSubject();
            if (subject == null || subject.isBlank() || subject.length() > 160) {
                throw new IllegalArgumentException("MLOps 登录令牌无效或已过期");
            }
            return subject;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("MLOps 登录令牌无效或已过期", exception);
        }
    }
}
