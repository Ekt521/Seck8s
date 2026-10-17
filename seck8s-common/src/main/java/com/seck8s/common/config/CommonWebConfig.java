package com.seck8s.common.config;

import com.seck8s.common.exception.GlobalExceptionHandler;
import com.seck8s.common.web.FeignHeaderInterceptor;
import com.seck8s.common.web.UserContextInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Web 业务服务公共配置组装点：全局异常 + 用户上下文拦截器 + Feign 身份透传。
 * 业务服务通过扫描 com.seck8s.common.config 引入；WebFlux 网关不加载本类。
 */
@Configuration
@Import({GlobalExceptionHandler.class, MybatisPlusConfig.class})
public class CommonWebConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new UserContextInterceptor()).addPathPatterns("/**");
    }

    @Bean
    public FeignHeaderInterceptor feignHeaderInterceptor() {
        return new FeignHeaderInterceptor();
    }
}
