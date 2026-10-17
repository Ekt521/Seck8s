package com.seck8s.user;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.seck8s.user",
        "com.seck8s.common.config",  // CommonWebConfig（全局异常/用户上下文/Feign透传）
        "com.seck8s.common.util"     // JwtUtil / OrderNoGenerator
})
@MapperScan("com.seck8s.user.mapper")
public class UserServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(UserServiceApplication.class, args);
    }
}
