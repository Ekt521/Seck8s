package com.seck8s.notification;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = {
        "com.seck8s.notification",
        "com.seck8s.common.config",
        "com.seck8s.common.util"
})
@MapperScan("com.seck8s.notification.mapper")
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
