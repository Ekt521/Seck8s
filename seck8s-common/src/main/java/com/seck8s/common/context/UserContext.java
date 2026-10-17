package com.seck8s.common.context;

import com.seck8s.common.exception.BizException;
import com.seck8s.common.result.ResultCode;

/**
 * 当前登录用户上下文：由网关注入的 X-User-Id 请求头还原，ThreadLocal 保存
 */
public class UserContext {

    private static final ThreadLocal<Long> HOLDER = new ThreadLocal<>();

    public static void set(Long userId) {
        HOLDER.set(userId);
    }

    public static Long get() {
        return HOLDER.get();
    }

    /** 取当前用户ID，未登录抛异常 */
    public static Long requireUserId() {
        Long userId = HOLDER.get();
        if (userId == null) {
            throw new BizException(ResultCode.UNAUTHORIZED, "未登录或登录已过期");
        }
        return userId;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
