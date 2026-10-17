package com.seck8s.gateway.filter;

import com.seck8s.common.util.JwtUtil;
import com.seck8s.gateway.config.Seck8sGatewayProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 网关 JWT 认证过滤器：
 * 白名单（登录/注册/公开查票）放行；其余校验 Bearer token，通过后注入 X-User-Id 头
 *
 * - 实现 GlobalFilter 和 Ordered，以全局拦截所有请求。
 * - 使用 AntPathMatcher 支持通配符路径匹配（如 /api/users/register 等）。
 * - 验证通过后，将 userId 添加到请求头 X-User-Id 中，传递给下游微服务（通过网关路由转发）。
 * - 若验证失败，直接返回 401 并终止请求，不再转发。
 */
@Slf4j
@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;
    private final Seck8sGatewayProperties seck8sGatewayProperties; // 读取白名单配置
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    public JwtAuthGlobalFilter(JwtUtil jwtUtil, Seck8sGatewayProperties seck8sGatewayProperties) {
        this.jwtUtil = jwtUtil;
        this.seck8sGatewayProperties = seck8sGatewayProperties;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();
        if (exchange.getRequest().getMethod() == org.springframework.http.HttpMethod.OPTIONS) {
            return chain.filter(exchange);
        }

        // 白名单直接放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 2. 提取 Authorization 头，必须是 "Bearer <token>"
        String authHeader = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return unauthorized(exchange, "未登录或缺少令牌");
        }
        String token = authHeader.substring(7);

        try {
            // 3. 解析 JWT 获取 userId
            Long userId = jwtUtil.parseUserId(token);
            // 4. 将 userId 以 Header 形式透传给下游（后续服务通过 UserContextInterceptor 读取）
            ServerHttpRequest mutated = exchange.getRequest().mutate()
                    .header("X-User-Id", String.valueOf(userId))
                    .build();
            return chain.filter(exchange.mutate().request(mutated).build());
        } catch (Exception e) {
            log.warn("JWT 校验失败 path={} err={}", path, e.getMessage());
            return unauthorized(exchange, "登录已过期，请重新登录");
        }
    }

    private boolean isWhitelisted(String path) {
        // 从配置文件中读取白名单列表，支持 Ant 风格路径（如 /api/users/register、/api/users/login）
        for (String pattern : seck8sGatewayProperties.getWhitelist()) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private Mono<Void> unauthorized(ServerWebExchange exchange, String msg) {
        // 响应式环境下的返回方式：设置状态码 401，直接写 JSON 字符串
        exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
        exchange.getResponse().getHeaders().add("Content-Type", "application/json;charset=UTF-8");
        String body = "{\"code\":401,\"message\":\"" + msg + "\"}";
        byte[] bytes = body.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return exchange.getResponse().writeWith(reactor.core.publisher.Mono.just(
                exchange.getResponse().bufferFactory().wrap(bytes)));
    }

    @Override
    public int getOrder() {
        return -100; // 优先级最高，最先执行，确保后面的过滤器能拿到 X-User-Id
    }
}
