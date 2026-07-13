package com.eshop;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;

/**
 * AuthUtil — 认证工具类
 *
 * 类比 Python backend/auth.py，包含 JWT 和密码加密
 *
 * Python auth.py:
 *   create_access_token(data)  → generateToken(userId, roles)
 *   decode_token(token)        → parseToken(token) → getUserId / getRoles
 *   hashlib.sha256()           → PasswordUtil.hash()
 */
@Component
public class AuthUtil {

    private final SecretKey key;
    private final long expirationMs;

    // Spring 自动从 application.yml 读取 jwt.secret 和 jwt.expiration
    public AuthUtil(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expiration) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expiration * 1000;
    }

    /* ==================== JWT ==================== */

    /** 生成 Token — 类比 Python: jwt.encode(payload, SECRET_KEY, algorithm=ALGORITHM) */
    public String generateToken(Integer userId, String roles) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("roles", roles)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(key)
                .compact();
    }

    /** 从 Token 提取用户ID — 类比 Python: payload.get("sub") */
    public Integer getUserIdFromToken(String token) {
        return Integer.valueOf(parseToken(token).getSubject());
    }

    /** 从 Token 提取角色 — 类比 Python: payload.get("roles") */
    public String getRolesFromToken(String token) {
        return parseToken(token).get("roles", String.class);
    }

    /** 解析 Token — 类比 Python: jwt.decode(token, SECRET_KEY, algorithms=[ALGORITHM]) */
    private Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /* ==================== 密码加密 ==================== */

    /** SHA-256 哈希 — 类比 Python: hashlib.sha256(password.encode('utf-8')).hexdigest() */
    public static String hashPassword(String password) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : hashBytes) {
                String h = Integer.toHexString(0xff & b);
                if (h.length() == 1) hex.append('0');
                hex.append(h);
            }
            return hex.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 不可用", e);
        }
    }

    /** 校验密码 — 类比 Python: result[2] == hash(password) */
    public static boolean verifyPassword(String rawPassword, String hashedPassword) {
        return hashPassword(rawPassword).equals(hashedPassword);
    }
}
