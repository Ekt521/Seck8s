package com.seck8s.payment;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {
        "com.seck8s.payment",
        "com.seck8s.common.config",
        "com.seck8s.common.util"
})
@MapperScan("com.seck8s.payment.mapper")
@EnableFeignClients(basePackages = "com.seck8s.payment.feign")
public class PaymentServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }
}
