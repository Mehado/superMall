package com.supermall.mall.utils;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.TypeReference;
import com.supermall.mall.constant.MallRedisKeyConstant;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.ThreadLocalRandom;

/**
 * 缓存工具类（电商版）：Cache-Aside 的手动实现，与外卖的 {@code takeout/utils/RedisCacheClient}
 * 是**两个独立的类**（有意复制，不共用 —— 两个板块必须能各自独立部署、独立回滚）。
 *
 * <p>三防：
 * <ul>
 *   <li><b>防穿透</b>：查不到的 key 写 {@link #NULL_MARK} 短 TTL，挡住反复打 DB 的无效请求；</li>
 *   <li><b>防击穿</b>：热点 key 过期时用 {@code setnx} 互斥锁 + 双重检查，只放一个请求回源；</li>
 *   <li><b>防雪崩</b>：写缓存时 TTL 加随机抖动，避免同一批 key 同时失效。</li>
 * </ul>
 *
 * <p>⚠️ 锁 key 一律带 {@link MallRedisKeyConstant#CACHE_LOCK_PREFIX}（{@code mall:cache:lock:}），
 * 绝不能与外卖的 {@code cache:lock:} 重名，否则两个板块会互相释放对方的锁。
 */
@Component
@Slf4j
@RequiredArgsConstructor
public class MallRedisCacheClient {

    /** 空值缓存标记：读到它等同于「确认无数据」，防止缓存穿透 */
    public static final String NULL_MARK = "NULL";

    private final StringRedisTemplate stringRedisTemplate;
    /** 释放锁脚本：由 MallRedisScriptConfig 从 scripts/mall_unlock.lua 加载（按字段名匹配 Bean） */
    private final DefaultRedisScript<Long> mallUnlockScript;

    /**
     * 写 JSON 缓存，随机 TTL 防雪崩。
     *
     * @param baseSeconds   基础过期秒数
     * @param jitterSeconds 随机抖动区间，最终 TTL = base + [0, jitter)
     */
    public void setJson(String key, Object value, long baseSeconds, long jitterSeconds) {
        String json = JSON.toJSONString(value);
        long ttl = baseSeconds + ThreadLocalRandom.current().nextLong(jitterSeconds);
        stringRedisTemplate.opsForValue().set(key, json, Duration.ofSeconds(ttl));
        log.debug("[mall] 缓存写入：key={}, ttl={}s", key, ttl);
    }

    /** 写空值标记，短 TTL 防穿透 */
    public void setNull(String key, long seconds) {
        stringRedisTemplate.opsForValue().set(key, NULL_MARK, Duration.ofSeconds(seconds));
        log.debug("[mall] 空值缓存写入：key={}, ttl={}s", key, seconds);
    }

    /** 读 JSON 缓存；未命中或命中空值标记时返回 null（调用方按「无缓存」走 DB） */
    public <T> T getJson(String key, TypeReference<T> type) {
        String json = stringRedisTemplate.opsForValue().get(key);
        if (json == null || NULL_MARK.equals(json) || "null".equals(json)) {
            return null;
        }
        return JSON.parseObject(json, type);
    }

    /** 删缓存（Cache-Aside 的「先写库，再删缓存」） */
    public void delete(String... keys) {
        if (keys != null && keys.length > 0) {
            stringRedisTemplate.delete(Arrays.asList(keys));
        }
    }

    /** 是否命中空值标记（区分「缓存未命中」与「缓存了不存在的空值」） */
    public boolean isNullMarked(String key) {
        return NULL_MARK.equals(stringRedisTemplate.opsForValue().get(key));
    }

    /** 尝试获取互斥锁：setnx + 过期时间，原子操作 */
    public boolean tryLock(String key, long expireSeconds) {
        String lockKey = MallRedisKeyConstant.CACHE_LOCK_PREFIX + key;
        Boolean ok = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, Thread.currentThread().getName(), Duration.ofSeconds(expireSeconds));
        return Boolean.TRUE.equals(ok);
    }

    /** 释放互斥锁：Lua 比较 value，只释放自己持有的锁 */
    public void unlock(String key) {
        String lockKey = MallRedisKeyConstant.CACHE_LOCK_PREFIX + key;
        stringRedisTemplate.execute(mallUnlockScript, Collections.singletonList(lockKey),
                Thread.currentThread().getName());
    }
}
