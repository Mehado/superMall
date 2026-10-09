package com.supermall.mall.constant;

/**
 * 电商板块 Redis key 统一收敛。
 *
 * <p>⚠️ **所有 key 都必须带 {@code mall:} 前缀**。
 * 外卖服务用的是 {@code stock:sku:{id}} / {@code order:submit:token:{token}} /
 * {@code merchant:business:{id}} 这类没有命名空间的 key；如果电商也用同名 key，
 * 两边的库存/令牌会互相覆盖 —— 属于线上事故级别的问题。
 */
public class MallRedisKeyConstant {

    private MallRedisKeyConstant() {
    }

    /** 验证码：mall:captcha:{key} */
    public static final String CAPTCHA_PREFIX = "mall:captcha:";

    /** SPU 商品详情缓存：mall:product:detail:{productId}（公开目录用，见 MallProductServiceImpl#detailCached） */
    public static final String PRODUCT_CACHE_PREFIX = "mall:product:detail:";

    /** 缓存互斥锁：mall:cache:lock:{业务key}（防击穿；释放走 mall_unlock.lua） */
    public static final String CACHE_LOCK_PREFIX = "mall:cache:lock:";

    /* ---------- 以下为后续阶段预留，命名已避开外卖 ---------- */

    /** SKU 库存：mall:stock:sku:{skuId} */
    public static final String SKU_STOCK_PREFIX = "mall:stock:sku:";

    /** 下单幂等令牌：mall:order:submit:token:{token} */
    public static final String SUBMIT_TOKEN_PREFIX = "mall:order:submit:token:";

    /** 支付幂等令牌：mall:pay:token:{token} */
    public static final String PAY_TOKEN_PREFIX = "mall:pay:token:";

    /** 缓存互斥锁：mall:cache:lock:{key} */

    /** 接口限流：mall:rate:limit:{ip}:{method}:{uri} */
    public static final String RATE_LIMIT_PREFIX = "mall:rate:limit:";

    /** 秒杀闸门（进阶）：mall:seckill:gate:{activityId} */
    public static final String SECKILL_GATE_PREFIX = "mall:seckill:gate:";
}
