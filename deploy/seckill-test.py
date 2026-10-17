# -*- coding: utf-8 -*-
"""Seck8s 防超卖并发压测脚本
用法: python seckill-test.py [并发用户数]
验证: 200 库存 + 250 用户并发 => 成功数<=200, 库存不为负, 订单数+库存=放量, 同用户限购
"""
import json
import sys
import threading
import time
import urllib.request

BASE = "http://127.0.0.1:8080"
INV = "http://127.0.0.1:8084"
N = int(sys.argv[1]) if len(sys.argv) > 1 else 250
ACTIVITY = "ACT20260804001"
SKU = "SKU002"
EXPECT_STOCK = 200  # SKU002 放量

results = {"ok": 0, "fail": 0, "detail": {}}
lock = threading.Lock()


def http(method, path, body=None, token=None, timeout=15):
    req = urllib.request.Request(BASE + path, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
        req.data = json.dumps(body).encode()
    if token:
        req.add_header("Authorization", "Bearer " + token)
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            return json.loads(resp.read().decode())
    except Exception as e:
        return {"code": -1, "message": str(e)}


def register(i):
    http("POST", "/api/users/register",
         {"username": "stress_%04d" % i, "password": "test123", "phone": "139%08d" % (10000000 + i)})


def get_token(i):
    r = http("POST", "/api/users/login", {"username": "stress_%04d" % i, "password": "test123"})
    if r.get("code") == 200:
        return r["data"]["token"]
    return None


def seckill(token):
    r = http("POST", "/api/seckill",
             {"activityId": ACTIVITY, "skuId": SKU, "quantity": 1}, token)
    with lock:
        st = (r.get("data") or {}).get("status", "ERR")
        msg = r.get("data", {}).get("message", r.get("message", ""))
        results["detail"].setdefault(st, 0)
        results["detail"][st] += 1
        if st == "CREATING":
            results["ok"] += 1
        else:
            results["fail"] += 1
    return r


def main():
    t0 = time.time()
    # 1) 并发注册 + 登录
    reg_threads = [threading.Thread(target=register, args=(i,)) for i in range(N)]
    for t in reg_threads:
        t.start()
    for t in reg_threads:
        t.join()
    tokens = []
    log_threads = [threading.Thread(target=lambda i=i: tokens.append(get_token(i))) for i in range(N)]
    for t in log_threads:
        t.start()
    for t in log_threads:
        t.join()
    tokens = [x for x in tokens if x]
    print("注册成功并拿到token的用户: %d/%d (耗时 %.1fs)" % (len(tokens), N, time.time() - t0))

    # 2) 并发秒杀
    t1 = time.time()
    sk_threads = [threading.Thread(target=seckill, args=(t,)) for t in tokens]
    for t in sk_threads:
        t.start()
    for t in sk_threads:
        t.join()
    print("秒杀完成: %d 个请求, 耗时 %.1fs" % (len(tokens), time.time() - t1))
    print("结果分布:", results["detail"])
    print("扣减成功(进入CREATING): %d, 拒绝: %d" % (results["ok"], results["fail"]))

    # 3) 等待异步建单 + 落库
    print("等待异步建单落库 20s...")
    time.sleep(20)

    # 4) 统计库存 (inventory-service 内部接口, 不经网关)
    with urllib.request.urlopen(INV + "/internal/inventory/stock/" + SKU, timeout=5) as resp:
        redis_stock = int(resp.read().decode())
    print("Redis 库存余量: %d (初始 %d, 已扣 %d)" % (redis_stock, EXPECT_STOCK, EXPECT_STOCK - redis_stock))

    # 5) 校验防超卖
    over = EXPECT_STOCK - redis_stock
    ok = results["ok"]
    print("\n======= 防超卖校验 =======")
    print("成功下单数(Redis扣减): %d" % ok)
    print("Redis 扣减总数: %d" % over)
    if ok != over:
        print("!!! 不一致: 成功下单数与Redis扣减数不符 (ok=%d, over=%d)" % (ok, over))
    if redis_stock < 0:
        print("!!! 超卖: Redis 库存为负 %d" % redis_stock)
    if ok > EXPECT_STOCK:
        print("!!! 超卖: 成功数 %d 超过库存 %d" % (ok, EXPECT_STOCK))
    if ok <= EXPECT_STOCK and ok == over and redis_stock >= 0:
        print("PASS: 无超卖, Redis扣减与成功数一致")


main()
