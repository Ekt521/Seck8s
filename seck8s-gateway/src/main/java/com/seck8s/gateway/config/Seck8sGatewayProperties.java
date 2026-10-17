package com.seck8s.gateway.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 网关业务配置：免认证白名单等。
 * 用 @ConfigurationProperties 绑定 YAML 列表，比 @Value 更可靠。
 */
@Data
@Component
@ConfigurationProperties(prefix = "seck8s.gateway")
public class Seck8sGatewayProperties {

    /** 免认证白名单（Ant 路径模式），默认空 */
    private List<String> whitelist = new ArrayList<>();

    /** 秒杀限流：窗口内最大请求数 */
    private int seckillLimit = 5;

    /** 秒杀限流窗口（秒） */
    private long seckillWindowSeconds = 1;
}
