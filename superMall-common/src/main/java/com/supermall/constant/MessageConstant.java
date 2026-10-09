package com.supermall.constant;

/**
 * 业务提示信息常量
 */
public class MessageConstant {

    public static final String PASSWORD_ERROR = "密码错误";
    public static final String ACCOUNT_NOT_FOUND = "账号不存在";
    public static final String ACCOUNT_LOCKED = "账号已被禁用";
    public static final String UNKNOWN_ERROR = "未知错误";
    public static final String USER_NOT_LOGIN = "用户未登录";
    public static final String LOGIN_FAILED = "登录失败";
    public static final String USERNAME_EXISTS = "该邮箱已被注册";
    public static final String PASSWORD_EDIT_FAILED = "密码修改失败";

    /* ---------- 图形验证码 ---------- */
    public static final String CAPTCHA_REQUIRED = "请先获取图形验证码";
    public static final String CAPTCHA_EXPIRED = "验证码已过期或已使用，请点击图片刷新";
    public static final String CAPTCHA_ERROR = "验证码不正确";

    public static final String BUSINESS_TYPE_INVALID = "业态类型不合法";
    public static final String CATEGORY_NOT_FOUND = "分类不存在";
    public static final String CATEGORY_NAME_EXISTS = "分类名称已存在";
    public static final String CATEGORY_RELATED_BY_PRODUCT = "当前分类下存在商品，不能删除";

    public static final String PRODUCT_NOT_FOUND = "商品不存在";
    public static final String PRODUCT_OFF_SALE = "商品已下架";
    public static final String PRODUCT_ON_SALE = "在售商品不能删除";
    public static final String STOCK_NOT_ENOUGH = "商品库存不足";
    public static final String STOCK_NOT_WARMED = "库存未预热，请稍后重试";

    public static final String ADDRESS_NOT_FOUND = "收货地址不存在";
    public static final String ADDRESS_IS_NULL = "收货地址为空，不能下单";

    public static final String CART_IS_NULL = "购物车数据为空，不能下单";
    public static final String CART_ITEM_NOT_FOUND = "购物车商品不存在";

    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_STATUS_ERROR = "订单状态错误";
    public static final String ORDER_CANCEL_BY_USER = "用户取消订单";
    public static final String ORDER_TIMEOUT_CANCEL = "订单超时未支付，自动取消";
    public static final String ORDER_REPEAT_SUBMIT = "订单已提交，请勿重复下单";
    public static final String ORDER_OUT_OF_DELIVERY_RANGE = "超出配送范围";
    public static final String ORDER_NO_PERMISSION = "无权操作该订单";

    public static final String PAY_TOKEN_INVALID = "支付令牌无效或已过期";
    public static final String PAY_ORDER_STATUS_ERROR = "订单不是待支付状态";

    public static final String DELIVERY_ORDER_NOT_FOUND = "配送单不存在";
    public static final String DELIVERY_ORDER_TAKEN = "手慢了，配送单已被抢走";
    public static final String DELIVERY_STATUS_ERROR = "配送单状态错误";
    public static final String DELIVERY_NO_PERMISSION = "无权操作该配送单";

    public static final String MERCHANT_NOT_FOUND = "商家不存在";
    public static final String MERCHANT_DISABLED = "商家已停业";
    public static final String MERCHANT_CLOSED = "商家休息中，暂不接单";
    public static final String BUSINESS_STATUS_INVALID = "营业状态不合法";

    /* ---------- 平台运营：商家入驻审核 / 骑手管理 ---------- */
    /** 商家入驻申请审核中（登录时拦下） */
    public static final String MERCHANT_AUDIT_PENDING = "店铺入驻申请审核中，请耐心等待平台审核";
    /** 商家入驻申请被驳回（登录时拦下，文案前缀，后面拼驳回原因） */
    public static final String MERCHANT_AUDIT_REJECTED = "店铺入驻申请未通过";
    /** 入驻时邮箱已被占用 */
    public static final String MERCHANT_APPLY_EXISTS = "该邮箱已提交过入驻申请";
    /** 审核入参非法 */
    public static final String AUDIT_STATUS_INVALID = "审核结果不合法";
    /** 驳回必须填原因 */
    public static final String AUDIT_REASON_REQUIRED = "驳回时必须填写驳回原因";
    public static final String MERCHANT_ALREADY_AUDITED = "该入驻申请已审核过，不能重复审核";

    public static final String PLATFORM_ADMIN_NOT_FOUND = "平台管理员账号不存在";
    public static final String PLATFORM_ADMIN_DISABLED = "平台管理员账号已被禁用";

    /* ---------- 头像上传（买家，两个板块共用文案） ---------- */
    public static final String AVATAR_EMPTY = "请选择要上传的头像图片";
    public static final String AVATAR_TYPE_INVALID = "头像只能是图片（jpg / png / gif / webp / bmp）";
    public static final String AVATAR_TOO_LARGE = "头像图片不能超过 2MB";
    public static final String AVATAR_UPLOAD_FAILED = "头像上传失败，请稍后重试";
    public static final String AVATAR_URL_INVALID = "头像地址格式不正确";

    public static final String DELIVERYMAN_NOT_FOUND = "配送员不存在";
    public static final String DELIVERYMAN_USERNAME_EXISTS = "该邮箱已被注册";
    public static final String DELIVERYMAN_STATUS_INVALID = "配送员状态只能是 1（可接单）或 0（停用）";
}
