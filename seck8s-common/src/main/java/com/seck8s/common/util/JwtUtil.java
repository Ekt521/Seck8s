package com.seck8s.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

/**
 * JWT 签发与校验（HS256）。密钥长度需 >= 32 字节。
 * 被网关（校验）和 user-service（签发）共用。
 * 因为加了 @Component，Spring 会管理它的生命周期，后面哪里需要直接 @Autowired 即可。
 */
@Component
public class JwtUtil {

    // 从配置文件读取 JWT 密钥。如果配置文件没写，就使用后面的默认硬编码密钥
    @Value("${seck8s.jwt.secret}")
    private String secret;

    // 令牌有效期（分钟），默认 120
    @Value("${seck8s.jwt.expire-minutes:120}")
    private long expireMinutes;

     /**
     * 将字符串密钥转换为 Java 加密库需要的 SecretKey 对象。
     * Keys.hmacShaKeyFor 是 JJWT 0.12.x 推荐的标准写法，它会自动校验密钥长度是否足够（< 32 字节会抛异常）。
     */
    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 生成 JWT 令牌。
     * 标准 JWT 包含：Header（签名算法）、Payload（数据）、Signature（签名）。
     * 这里我们把 userId 放入 subject（主题）字段，把 username 放入自定义的 claim（载荷）。
     */
    public String generateToken(Long userId, String username) {
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .subject(String.valueOf(userId))            // 设置主题，放用户唯一标识
                .claim("username", username)                // 额外携带的用户名信息
                .issuedAt(new Date(now))                    // 签发时间（iat）
                .expiration(new Date(now + expireMinutes * 60_000L)) // 过期时间（exp），毫秒计算
                .signWith(key())                            // 使用 HS256 签名
                .compact();                                 // 压缩成最终的 Token 字符串
    }

    /** 解析并返回 userId；令牌非法/过期抛 JwtException */
    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }

    /** 校验令牌是否合法且未过期 */
    public boolean validate(String token) {
        try {
            parseUserId(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
