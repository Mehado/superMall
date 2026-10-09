package com.supermall.mall.constant;

import com.supermall.constant.RoleConstant;

/**
 * 电商板块的提示文案与角色常量。
 *
 * <p>买家相关的文案直接复用 {@link com.supermall.constant.MessageConstant}
 * （账号不存在 / 密码错误 / 验证码不正确 等两边语义一致），
 * 这里只放电商特有的部分，避免复制一份完整的 MessageConstant 造成维护分裂。
 */
public class MallMessageConstant {

    private MallMessageConstant() {
    }

    /* ---------- 角色 ---------- */

    /**
     * 运营后台管理员。
     * 刻意不往 common 的 RoleConstant 里加 —— 不动外卖已经在用的共享类。
     */
    public static final String ADMIN = "MALL_ADMIN";

    /* ---------- 管理员 ---------- */

    public static final String ADMIN_NOT_FOUND = "管理员账号不存在";
    public static final String ADMIN_DISABLED = "管理员账号已被禁用";

    /* ---------- 买家（复用外卖文案前的补充） ---------- */

    public static final String USER_NOT_FOUND = "账号不存在";

    /* ---------- P1 品牌 ---------- */

    public static final String BRAND_NOT_FOUND = "品牌不存在";
    public static final String BRAND_NAME_EXISTS = "品牌名已存在";
    public static final String BRAND_IN_USE = "该品牌下还有商品，不能删除";

    /* ---------- P1 分类 ---------- */

    public static final String CATEGORY_NOT_FOUND = "分类不存在";
    public static final String CATEGORY_NAME_EXISTS = "同级下已存在同名分类";
    public static final String CATEGORY_HAS_CHILDREN = "请先删除子分类";
    public static final String CATEGORY_HAS_PRODUCT = "该分类下还有商品，不能删除";
    public static final String CATEGORY_MAX_LEVEL = "分类最多 3 级";
    public static final String CATEGORY_NOT_LEAF = "商品只能挂在最后一级分类上";
    public static final String CATEGORY_PARENT_SELF = "不能把分类挂到自己或自己的子分类下";

    /* ---------- P1 商品 / SKU ---------- */

    public static final String PRODUCT_NOT_FOUND = "商品不存在";
    public static final String PRODUCT_ON_SALE_CANNOT_DELETE = "上架中的商品不能删除，请先下架";
    public static final String PRODUCT_IMAGE_TOO_MANY = "商品图片最多 10 张";
    public static final String SPEC_TOO_MANY = "规格项最多 3 个";
    public static final String SPEC_NAME_DUPLICATE = "规格名不能重复";
    public static final String SPEC_VALUE_EMPTY = "每个规格至少要有 1 个规格值";
    public static final String SPEC_VALUE_DUPLICATE = "同一规格下的规格值不能重复";
    public static final String SPEC_VALUE_UNKNOWN = "提交的规格值不在该商品的规格里";
    public static final String SKU_MATRIX_MISMATCH = "SKU 组合数不对：应等于各规格值数量的乘积";
    public static final String SKU_DUPLICATE = "存在重复的规格组合";
    public static final String SKU_REQUIRED = "至少要有一个 SKU";
    public static final String SKU_NO_SPEC_EXACTLY_ONE = "无规格商品只能有 1 个 SKU";

    /* ---------- P3 购物车 ---------- */

    public static final String CART_ITEM_NOT_FOUND = "购物车中不存在该商品";
    public static final String CART_SKU_NOT_FOUND = "商品规格不存在或已失效";
    public static final String CART_PRODUCT_OFF_SALE = "商品已下架，无法加入购物车";
    public static final String CART_SKU_DISABLED = "该规格已停用";
    public static final String CART_OUT_OF_STOCK = "商品已售罄";
    public static final String CART_STOCK_NOT_ENOUGH = "库存不足，最多可购 ";
    public static final String CART_ITEM_INVALID = "该商品已失效，不能选中";
    public static final String CART_SCOPE_INVALID = "清理范围只能是 all 或 invalid";

    /* ---------- P3 收货地址 / 行政区划 ---------- */

    public static final String ADDRESS_NOT_FOUND = "收货地址不存在";
    public static final String ADDRESS_LIMIT = "最多只能保存 20 个收货地址";
    public static final String ADDRESS_AREA_REQUIRED = "请选择完整的省市区";
    public static final String AREA_NOT_FOUND = "行政区划不存在";
    public static final String AREA_NOT_LEAF = "请选择到区/县一级";

    /* ---------- P4 库存 ---------- */

    public static final String STOCK_DEDUCT_ERROR = "库存扣减异常，请稍后重试";
    public static final String STOCK_NOT_WARMED = "库存未预热，请稍后重试";
    public static final String STOCK_NOT_ENOUGH = "库存不足";

    /* ---------- P4 订单 ---------- */

    public static final String ORDER_NOT_FOUND = "订单不存在";
    public static final String ORDER_NO_PERMISSION = "无权操作该订单";
    public static final String ORDER_REPEAT_SUBMIT = "请勿重复提交订单";
    public static final String ORDER_STATUS_ERROR = "订单状态不允许该操作";
    public static final String ORDER_CART_EMPTY = "请先选择要结算的商品";
    public static final String ORDER_CANCEL_BY_USER = "买家取消订单";
    public static final String ORDER_TIMEOUT_CANCEL = "超时未支付，系统自动取消";
    public static final String ORDER_ITEM_INVALID_PREFIX = "「";
    public static final String ORDER_ALREADY_SHIPPED = "该订单已发货，不能重复发货";

    /* ---------- P4 支付 ---------- */

    public static final String PAY_ORDER_STATUS_ERROR = "订单状态不允许支付";
    public static final String PAY_TOKEN_INVALID = "支付令牌无效或已使用，请重新发起支付";

    /* ---------- P4 退款 ---------- */

    public static final String REFUND_NOT_FOUND = "退款申请不存在";
    public static final String REFUND_NOT_ALLOWED = "当前订单状态不支持申请退款";
    public static final String REFUND_TYPE_INVALID = "退款类型只能是 1（仅退款）或 2（退货退款）";
    public static final String REFUND_STATUS_ERROR = "该退款申请已审核，不能重复审核";

    /* ---------- P5 评价 ---------- */

    public static final String REVIEW_NOT_FOUND = "评价不存在";
    public static final String REVIEW_NOT_ALLOWED = "只有已完成的订单才能评价";
    public static final String REVIEW_ALREADY = "该商品已评价过，不能重复评价";
    public static final String REVIEW_ALREADY_FOLLOWED = "已追评过，不能重复追评";
    public static final String REVIEW_ITEM_NOT_IN_ORDER = "评价的商品不属于该订单";
    public static final String REVIEW_DUPLICATE_ITEM = "同一明细不能重复评价";
    public static final String REVIEW_TOO_MANY_IMAGES = "晒图最多 9 张";

    /* ---------- P7 会员 / 统计 ---------- */

    public static final String MEMBER_NOT_FOUND = "会员不存在";
    public static final String MEMBER_STATUS_INVALID = "会员状态只能是 1（启用）或 0（禁用）";
}
