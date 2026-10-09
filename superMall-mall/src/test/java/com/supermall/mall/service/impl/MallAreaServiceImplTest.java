package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.vo.MallAreaResolvedVO;
import com.supermall.mall.vo.MallAreaVO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * 行政区划：加载真实的 mall-area.json，验证三级联动与末级校验。
 *
 * <p>不依赖数据库/网络，纯内存。
 */
@DisplayName("MallAreaServiceImpl - 省市区三级联动")
class MallAreaServiceImplTest {

    private final MallAreaServiceImpl service = new MallAreaServiceImpl();

    @Test
    @DisplayName("根节点返回 34 个省级行政区")
    void rootReturnsProvinces() {
        List<MallAreaVO> provinces = service.children(null);
        assertThat(provinces).hasSize(34);
        assertThat(provinces).extracting(MallAreaVO::getName).contains("北京市", "广东省", "香港特别行政区");
        // 省一定还有下级
        assertThat(provinces).allMatch(p -> Boolean.TRUE.equals(p.getHasChildren()) && p.getLevel() == 1);
    }

    @Test
    @DisplayName("逐级下钻：省 → 市 → 区县，末级 hasChildren=false")
    void drillDown() {
        List<MallAreaVO> cities = service.children("110000");
        assertThat(cities).extracting(MallAreaVO::getCode).containsExactly("110100");
        assertThat(cities.get(0).getName()).isEqualTo("北京市");

        List<MallAreaVO> districts = service.children("110100");
        assertThat(districts).isNotEmpty();
        assertThat(districts).extracting(MallAreaVO::getName).contains("东城区", "朝阳区");
        assertThat(districts).allMatch(d -> Boolean.FALSE.equals(d.getHasChildren()) && d.getLevel() == 3);
    }

    @Test
    @DisplayName("未知 parentCode 返回空列表，不抛异常")
    void unknownParentReturnsEmpty() {
        assertThat(service.children("999999")).isEmpty();
        assertThat(service.children("")).hasSize(34);
    }

    @Test
    @DisplayName("resolve：由区县 code 反查省 / 市 / 区名称")
    void resolveDistrict() {
        MallAreaResolvedVO r = service.resolve("110101");
        assertThat(r.getAreaCode()).isEqualTo("110101");
        assertThat(r.getProvince()).isEqualTo("北京市");
        assertThat(r.getCity()).isEqualTo("北京市");
        assertThat(r.getDistrict()).isEqualTo("东城区");
        assertThat(r.getFullName()).isEqualTo("北京市 北京市 东城区");
    }

    @Test
    @DisplayName("resolve：广东省深圳市南山区")
    void resolveShenzhen() {
        // 440300 = 深圳市，440305 = 南山区
        assertThat(service.exists("440300")).isTrue();
        List<MallAreaVO> districts = service.children("440300");
        assertThat(districts).extracting(MallAreaVO::getName).contains("南山区");
        MallAreaResolvedVO r = service.resolve("440305");
        assertThat(r.getProvince()).isEqualTo("广东省");
        assertThat(r.getCity()).isEqualTo("深圳市");
        assertThat(r.getDistrict()).isEqualTo("南山区");
    }

    @Test
    @DisplayName("resolve：末级是「市」（港澳）时 district 为空，但仍是合法地址")
    void resolveLeafCity() {
        MallAreaResolvedVO r = service.resolve("810001");
        assertThat(r.getProvince()).isEqualTo("香港特别行政区");
        assertThat(r.getCity()).isNotEmpty();
        assertThat(r.getDistrict()).isNull();
    }

    @Test
    @DisplayName("resolve：非末级 / 不存在 的 code 都被拒")
    void resolveRejectsNonLeaf() {
        assertThatThrownBy(() -> service.resolve("110000"))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.AREA_NOT_LEAF);

        assertThatThrownBy(() -> service.resolve("999999"))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.AREA_NOT_FOUND);

        assertThatThrownBy(() -> service.resolve(null))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.AREA_NOT_FOUND);
    }

    @Test
    @DisplayName("exists 带首尾空格也能识别")
    void existsTrims() {
        assertThat(service.exists(" 110101 ")).isTrue();
        assertThat(service.exists("abc")).isFalse();
        assertThat(service.exists(null)).isFalse();
    }
}
