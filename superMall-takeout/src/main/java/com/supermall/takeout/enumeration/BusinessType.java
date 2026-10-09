package com.supermall.takeout.enumeration;

/**
 * 平台业态：外卖食品 / 百货商品。
 * <p>
 * 品类归属由 {@code category.business_type} 决定；商品的业态由所选分类派生写入
 * {@code product.business_type}，服务端保证一致。
 */
public enum BusinessType {

    FOOD(1, "外卖食品"),
    GOODS(2, "百货商品");

    private final Integer code;
    private final String desc;

    BusinessType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    public Integer getCode() {
        return code;
    }

    public String getDesc() {
        return desc;
    }

    /** 校验业态编码是否合法 */
    public static boolean isValid(Integer code) {
        return ofCode(code) != null;
    }

    /** 按编码取枚举，非法返回 null */
    public static BusinessType ofCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (BusinessType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        return null;
    }
}
