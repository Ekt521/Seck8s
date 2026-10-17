package com.seck8s.activity;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.seck8s.activity",
        "com.seck8s.common.config",
        "com.seck8s.common.util"
})
@MapperScan("com.seck8s.activity.mapper")
@EnableFeignClients(basePackages = "com.seck8s.activity.feign")
public class ActivityServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(ActivityServiceApplication.class, args);
    }
}
