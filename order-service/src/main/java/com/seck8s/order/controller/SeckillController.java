package com.seck8s.order.controller;

import com.seck8s.common.context.UserContext;
import com.seck8s.common.result.Result;
import com.seck8s.order.dto.SeckillRequest;
import com.seck8s.order.dto.SeckillResult;
import com.seck8s.order.service.SeckillService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/seckill")
public class SeckillController {

    private final SeckillService seckillService;

    public SeckillController(SeckillService seckillService) {
        this.seckillService = seckillService;
    }

    @PostMapping
    public Result<SeckillResult> seckill(@RequestBody SeckillRequest request) {
        Long userId = UserContext.requireUserId();
        return Result.ok(seckillService.seckill(userId, request));
    }
}
