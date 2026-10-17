package com.seck8s.common.web;

import com.seck8s.common.context.UserContext;
import feign.RequestInterceptor;
import feign.RequestTemplate;

/**
 * OpenFeign 调用时透传当前用户身份头，保证下游服务能还原 UserContext
 */
public class FeignHeaderInterceptor implements RequestInterceptor {

    @Override
    public void apply(RequestTemplate template) {
        Long userId = UserContext.get();
        if (userId != null) {
            template.header(UserContextInterceptor.HEADER_USER_ID, String.valueOf(userId));
        }
    }
}
