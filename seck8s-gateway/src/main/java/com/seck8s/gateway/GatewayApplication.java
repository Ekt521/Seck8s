package com.seck8s.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.seck8s.gateway",
        "com.seck8s.common.util"   // 仅引入 JwtUtil 等纯工具，不加载 Web 业务配置
})
public class GatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(GatewayApplication.class, args);
    }
}
