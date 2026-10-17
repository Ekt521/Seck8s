package com.seck8s.gateway.filter;

import com.seck8s.gateway.config.Seck8sGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * 秒杀接口限流：同一用户在单位窗口内的秒杀请求频率限制（防刷保护）
 * 注意：不读取请求体（会破坏网关转发），按 userId 全局限流即可。
 * 结合 Redis Lua 的限购计数，构成"网关限流 + 业务限购"双保险。
 */
@Slf4j
@Component
public class SeckillRateLimitFilter implements GlobalFilter, Ordered {

    private static final String RATE_PREFIX = "seck8s:rate:";

    private final StringRedisTemplate redisTemplate;
    private final Seck8sGatewayProperties seck8sGatewayProperties;

    public SeckillRateLimitFilter(StringRedisTemplate redisTemplate, Seck8sGatewayProperties seck8sGatewayProperties) {
        this.redisTemplate = redisTemplate;
        this.seck8sGatewayProperties = seck8sGatewayProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        // 仅拦截秒杀接口
        if (!"/api/seckill".equals(path)) {
            return chain.filter(exchange);
        }

        String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
        if (!StringUtils.hasText(userId)) {
            return chain.filter(exchange); // 未带用户头（理论上不会发生，JWT 过滤器先执行）
        }

        String key = RATE_PREFIX + userId;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(seck8sGatewayProperties.getSeckillWindowSeconds()));
            }
            if (count != null && count > seck8sGatewayProperties.getSeckillLimit()) {
                return reject(exchange);
            }
        } catch (Exception e) {
            log.warn("限流检查异常，放行 path={}: {}", path, e.getMessage());
        }
        return chain.filter(exchange);
    }

    private Mono<Void> reject(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":429,\"message\":\"请求过于频繁，请稍后再试\"}";
        DataBuffer buffer = exchange.getResponse().bufferFactory()
                .wrap(body.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        return -50; // 在 JWT 之后
    }
}
