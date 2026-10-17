package com.seck8s.activity.service;

import com.seck8s.activity.entity.Activity;
import com.seck8s.activity.entity.ActivitySku;

import java.util.List;

public interface ActivityService {

    Activity createActivity(Activity activity);

    /** 发布活动：写入 Redis 活动/SKU 缓存，并触发 inventory 库存预热 */
    void publish(String activityId);

    void end(String activityId);

    List<Activity> listOngoing();

    Activity getActivity(String activityId);

    List<ActivitySku> listActivitySkus(String activityId);
}
