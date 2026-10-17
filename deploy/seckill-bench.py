# -*- coding: utf-8 -*-
"""Seck8s 秒杀防超卖 + 极限并发压测脚本
用法:
  python seckill-bench.py reset          # 仅重置 SKU002 库存
  python seckill-bench.py <并发用户数>    # 重置库存 + N 并发秒杀 + 防超卖全量校验

校验维度（每档）:
  1. Redis 库存余量 (seck8s:stock:SKU002)
  2. DB 库存 (sku_stock)
  3. 成功下单数 == 扣减数 == 放量 (200)
  4. stock_flow_log DEDUCT 流水 == 200
  5. 同用户重复下单 == 0
"""
import json
import os
import subprocess
import socket
import sys
import threading
import time
import urllib.request

BASE = "http://127.0.0.1:8080"
INV = "http://127.0.0.1:8084"
ACTIVITY = "ACT20260804001"
SKU = "SKU002"
EXPECT_STOCK = 200
# 压测用户: jmeter_0001~1000 (已预注册)
N = int(sys.argv[1]) if len(sys.argv) > 1 and sys.argv[1].isdigit() else 250

MYSQL = ["mysql", "-h127.0.0.1", "-P3306", "-uroot", "-N", "-B"]
MYSQL_PASSWORD = os.environ.get("SECK8S_MYSQL_PASSWORD", "")
if MYSQL_PASSWORD:
    MYSQL.insert(4, "-p" + MYSQL_PASSWORD)
REDIS_HOST = os.environ.get("SECK8S_REDIS_HOST", "10.163.55.111")
REDIS_PORT = int(os.environ.get("SECK8S_REDIS_PORT", "6379"))
REDIS_PASS = os.environ.get("SECK8S_REDIS_PASSWORD", "")

results = {"ok": 0, "fail": 0, "detail": {}, "latency": []}
lock = threading.Lock()


# ---------------- Redis (RESP) ----------------
class Resp:
    def __init__(self):
        self.s = socket.create_connection((REDIS_HOST, REDIS_PORT), timeout=5)
        if REDIS_PASS:
            self._cmd(["AUTH", REDIS_PASS])

    def _cmd(self, args):
        buf = b"*%d\r\n" % len(args)
        for a in args:
            a = str(a).encode()
            buf += b"$%d\r\n%s\r\n" % (len(a), a)
        self.s.sendall(buf)
        return self._read()

    def _read(self):
        head = self.s.recv(1)
        if not head:
            return None
        if head == b"+":
            return self._line()
        if head == b"-":
            return self._line()
        if head == b":":
            return int(self._line())
        if head == b"$":
            ln = int(self._line())
            if ln == -1:
                return None
            data = b""
            while len(data) < ln + 2:
                data += self.s.recv(ln + 2 - len(data))
            return data[:-2].decode()
        if head == b"*":
            n = int(self._line())
            return [self._read() for _ in range(n)]
        return head + self._line()

    def _line(self):
        buf = b""
        while not buf.endswith(b"\r\n"):
            buf += self.s.recv(1)
        return buf[:-2].decode()


def redis_get(key):
    return Resp()._cmd(["GET", key])


def redis_set(key, val):
    Resp()._cmd(["SET", key, str(val)])


def redis_del(prefix):
    r = Resp()
    keys = r._cmd(["KEYS", prefix + "*"])
    if keys:
        r._cmd(["DEL"] + keys)
        return len(keys)
    return 0


# ---------------- MySQL ----------------
def sql(query):
    return subprocess.run(MYSQL + ["-e", query], capture_output=True, text=True).stdout.strip()


def reset_stock():
    redis_set("seck8s:stock:" + SKU, EXPECT_STOCK)
    n = redis_del("seck8s:bought:" + ACTIVITY + ":")
    sql("UPDATE seck8s_inventory_db.sku_stock SET stock=%d, version=version+1 WHERE sku_id='%s'" % (EXPECT_STOCK, SKU))
    sql("DELETE FROM seck8s_inventory_db.stock_flow_log WHERE sku_id='%s'" % SKU)
    sql("DELETE FROM seck8s_order_db.tb_message_outbox WHERE biz_key IN (SELECT order_id FROM seck8s_order_db.orders WHERE sku_id='%s')" % SKU)
    sql("DELETE FROM seck8s_order_db.orders WHERE sku_id='%s'" % SKU)
    print("库存已重置: Redis=%s DB=200, 清空 bought/DEDUCT流水/历史订单" % redis_get("seck8s:stock:" + SKU))


# ---------------- 秒杀 ----------------
def http(method, path, body=None, token=None, timeout=15):
    req = urllib.request.Request(BASE + path, method=method)
    if body is not None:
        req.add_header("Content-Type", "application/json")
        req.data = json.dumps(body).encode()
    if token:
        req.add_header("Authorization", "Bearer " + token)
    t0 = time.time()
    try:
        with urllib.request.urlopen(req, timeout=timeout) as resp:
            dt = (time.time() - t0) * 1000
            with lock:
                results["latency"].append(dt)
            return json.loads(resp.read().decode())
    except Exception as e:
        return {"code": -1, "message": str(e)}


def get_token(i):
    r = http("POST", "/api/users/login", {"username": "jmeter_%04d" % i, "password": "test123"})
    if r.get("code") == 200:
        return r["data"]["token"]
    return None


def seckill(token, i):
    r = http("POST", "/api/seckill", {"activityId": ACTIVITY, "skuId": SKU, "quantity": 1}, token)
    with lock:
        st = (r.get("data") or {}).get("status", "ERR_%s" % r.get("code"))
        results["detail"].setdefault(st, 0)
        results["detail"][st] += 1
        if st == "CREATING":
            results["ok"] += 1
        else:
            results["fail"] += 1


def bench():
    t0 = time.time()
    tokens = []
    lt = [threading.Thread(target=lambda i=i: tokens.append((i, get_token(i)))) for i in range(1, N + 1)]
    for t in lt:
        t.start()
    for t in lt:
        t.join()
    tokens = [x for x in tokens if x[1]]
    print("登录成功: %d/%d (%.1fs)" % (len(tokens), N, time.time() - t0))

    t1 = time.time()
    st = [threading.Thread(target=seckill, args=(tok, i)) for i, tok in tokens]
    for t in st:
        t.start()
    for t in st:
        t.join()
    dt = time.time() - t1
    lat = sorted(results["latency"])
    p95 = lat[int(len(lat) * 0.95)]
    p99 = lat[min(len(lat) - 1, int(len(lat) * 0.99))]
    print("秒杀请求 %d 个, 耗时 %.1fs" % (len(tokens), dt))
    print("状态分布: %s" % json.dumps(results["detail"], ensure_ascii=False))
    print("扣减成功(CREATING): %d, 拒绝: %d" % (results["ok"], results["fail"]))
    print("延迟 avg=%.0fms p95=%.0fms p99=%.0fms" % (sum(lat) / len(lat), p95, p99))


def verify():
    print("\n等待异步建单落库 25s ...")
    time.sleep(25)
    r_stock = int(redis_get("seck8s:stock:" + SKU) or "-1")
    db_stock = sql("SELECT stock FROM seck8s_inventory_db.sku_stock WHERE sku_id='%s'" % SKU)
    order_cnt = sql("SELECT COUNT(*) FROM seck8s_order_db.orders WHERE sku_id='%s'" % SKU)
    deduct_cnt = sql("SELECT COUNT(*) FROM seck8s_inventory_db.stock_flow_log WHERE sku_id='%s' AND change_type='DEDUCT'" % SKU)
    dup = sql("SELECT COUNT(*) FROM (SELECT user_id FROM seck8s_order_db.orders WHERE sku_id='%s' GROUP BY user_id HAVING COUNT(*)>1) t" % SKU)
    db_neg = sql("SELECT COUNT(*) FROM seck8s_inventory_db.sku_stock WHERE sku_id='%s' AND stock<0" % SKU)

    print("\n=========== 防超卖校验 ===========")
    print("Redis 库存余量 : %s (期望 0)" % r_stock)
    print("DB 库存余量   : %s (期望 0)" % db_stock)
    print("订单数(落库) : %s (期望 %d)" % (order_cnt, EXPECT_STOCK))
    print("DEDUCT 流水  : %s (期望 %d)" % (deduct_cnt, EXPECT_STOCK))
    print("同用户重复下单: %s (期望 0)" % dup)
    print("DB 负库存行  : %s (期望 0)" % db_neg)

    ok = (results["ok"] == EXPECT_STOCK == int(order_cnt) == int(deduct_cnt)
          and int(r_stock) == 0 and int(db_stock) == 0 and int(dup) == 0 and int(db_neg) == 0)
    print("\n结论: %s" % ("[PASS] 无超卖，扣减/订单/流水全部一致" if ok else "[FAIL] 存在不一致，需检查!"))


def main():
    if len(sys.argv) > 1 and sys.argv[1] == "reset":
        reset_stock()
        return
    print(">>> 档位: %d 并发用户, 活动 %s, SKU %s, 库存 %d" % (N, ACTIVITY, SKU, EXPECT_STOCK))
    reset_stock()
    bench()
    verify()


if __name__ == "__main__":
    main()
