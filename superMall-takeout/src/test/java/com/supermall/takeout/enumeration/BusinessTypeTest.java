package com.supermall.takeout.enumeration;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("业态枚举 BusinessType")
class BusinessTypeTest {

    @Test
    @DisplayName("合法编码：1 外卖食品 / 2 百货商品")
    void isValid_acceptsKnownCodes() {
        assertThat(BusinessType.isValid(1)).isTrue();
        assertThat(BusinessType.isValid(2)).isTrue();
    }

    @Test
    @DisplayName("非法编码与 null 一律拒绝")
    void isValid_rejectsUnknownOrNull() {
        assertThat(BusinessType.isValid(0)).isFalse();
        assertThat(BusinessType.isValid(9)).isFalse();
        assertThat(BusinessType.isValid(-1)).isFalse();
        assertThat(BusinessType.isValid(null)).isFalse();
    }

    @Test
    @DisplayName("ofCode 映射正确，非法返回 null")
    void ofCode_mapsToEnum() {
        assertThat(BusinessType.ofCode(1)).isEqualTo(BusinessType.FOOD);
        assertThat(BusinessType.ofCode(2)).isEqualTo(BusinessType.GOODS);
        assertThat(BusinessType.ofCode(3)).isNull();
        assertThat(BusinessType.ofCode(null)).isNull();
    }

    @Test
    @DisplayName("编码与描述稳定（改动即为破坏性变更）")
    void codesAndDescsAreStable() {
        assertThat(BusinessType.FOOD.getCode()).isEqualTo(1);
        assertThat(BusinessType.FOOD.getDesc()).isEqualTo("外卖食品");
        assertThat(BusinessType.GOODS.getCode()).isEqualTo(2);
        assertThat(BusinessType.GOODS.getDesc()).isEqualTo("百货商品");
    }
}
