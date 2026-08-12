package cn.bvovd.clouddrive.utils;

import cn.bvovd.clouddrive.exception.BusinessException;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {

    // 从配置文件中读取密钥（也可以硬编码，但推荐配置）
    @Value("${jwt.secret:defaultSecretKey12345678901234567890123456789012}")
    private String secret;

    @Value("${jwt.expiration:86400000}") // 默认24小时（毫秒）
    private Long expiration;

    /**
     * 生成 JWT Token
     * @param userId 用户ID
     * @param phone 手机号
     * @return Token字符串
     */
    public String generateToken(Long userId, Integer role,String phone) {
        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

        return Jwts.builder()
                .subject(String.valueOf(userId))            // 设置主题（用户ID）
                .claim("phone", phone)
                .claim("role",role)// 自定义字段：手机号
                .issuedAt(now)                              // 签发时间
                .expiration(expiryDate)                     // 过期时间
                .signWith(key)                              // 签名算法（默认 HS256）
                .compact();
    }

    /**
     * 解析 Token，获取 Claims
     */
    public Claims parseToken(String token) {
        SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 从 Token 中获取用户ID
     */
    public Long getUserIdFromToken(String token) {
        Claims claims = parseToken(token);
        return Long.parseLong(claims.getSubject());
    }

    /**
     * 从 Token 中获取手机号
     */
    public String getPhoneFromToken(String token) {
        Claims claims = parseToken(token);
        return claims.get("phone", String.class);
    }
    /**
     * 从 Token 中获取身份
     */
    public Integer getRoleFromToken(String token) {
        Claims claims = parseToken(token);
        return Integer.parseInt(claims.getSubject());
    }

    /**
     * 校验 Token 是否有效（未过期且签名正确）
     */
    public boolean validateToken(String token) {
        try {
            parseToken(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public Long getUserIdFromRequest(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new BusinessException("未提供有效的认证凭证");
        }
        String token = authHeader.substring(7); // 去掉 "Bearer " 前缀
        return getUserIdFromToken(token); // 调用之前写好的解析方法
    }
}