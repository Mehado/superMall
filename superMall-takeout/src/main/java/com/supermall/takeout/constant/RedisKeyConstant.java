package com.supermall.takeout.constant;

/**
 * Redis key 统一收敛，避免散落在业务代码里。
 */
public class RedisKeyConstant {

    /** 商品缓存：product:detail:{productId} */
    public static final String PRODUCT_CACHE_PREFIX = "product:detail:";

    /** 商品库存：stock:product:{productId} */
    public static final String PRODUCT_STOCK_PREFIX = "stock:product:";

    /** 规格库存：stock:sku:{skuId} */
    public static final String SKU_STOCK_PREFIX = "stock:sku:";

    /** 下单幂等令牌：order:submit:token:{token} */
    public static final String SUBMIT_TOKEN_PREFIX = "order:submit:token:";

    /** 支付幂等令牌：pay:token:{token} */
    public static final String PAY_TOKEN_PREFIX = "pay:token:";

    /** 抢单幂等标记：delivery:grab:{orderId} */
    public static final String DELIVERY_GRAB_PREFIX = "delivery:grab:";

    /** 缓存互斥锁 */
    public static final String CACHE_LOCK_PREFIX = "cache:lock:";

    /** 商家营业状态：merchant:business:{merchantId} —— 1营业中 0休息中 */
    public static final String MERCHANT_BUSINESS_PREFIX = "merchant:business:";
}
