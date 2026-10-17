package com.seck8s.order;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.seck8s.order",
        "com.seck8s.common.config",
        "com.seck8s.common.util"
})
@MapperScan("com.seck8s.order.mapper")
@EnableFeignClients(basePackages = "com.seck8s.order.feign")
public class OrderServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(OrderServiceApplication.class, args);
    }
}
