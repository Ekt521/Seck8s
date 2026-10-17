package com.seck8s.activity.controller;

import com.seck8s.activity.entity.Activity;
import com.seck8s.activity.entity.ActivitySku;
import com.seck8s.activity.service.ActivityService;
import com.seck8s.common.result.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/activities")
public class ActivityController {

    private final ActivityService activityService;

    public ActivityController(ActivityService activityService) {
        this.activityService = activityService;
    }

    /** 管理员建活动（草稿） */
    @PostMapping
    public Result<Activity> create(@RequestBody Activity activity) {
        return Result.ok(activityService.createActivity(activity));
    }

    /** 发布活动：写缓存 + 触发库存预热 */
    @PostMapping("/{activityId}/publish")
    public Result<Void> publish(@PathVariable String activityId) {
        activityService.publish(activityId);
        return Result.ok();
    }

    /** 结束活动 */
    @PostMapping("/{activityId}/end")
    public Result<Void> end(@PathVariable String activityId) {
        activityService.end(activityId);
        return Result.ok();
    }

    /** 进行中的活动列表 */
    @GetMapping("/ongoing")
    public Result<List<Activity>> ongoing() {
        return Result.ok(activityService.listOngoing());
    }

    @GetMapping("/{activityId}")
    public Result<Activity> get(@PathVariable String activityId) {
        return Result.ok(activityService.getActivity(activityId));
    }

    /** 活动参与的 SKU 列表 */
    @GetMapping("/{activityId}/skus")
    public Result<List<ActivitySku>> skus(@PathVariable String activityId) {
        return Result.ok(activityService.listActivitySkus(activityId));
    }
}
