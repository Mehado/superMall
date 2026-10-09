package com.supermall.mall.service.impl;

import com.supermall.mall.dto.MallBannerDTO;
import com.supermall.mall.entity.MallBanner;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallBannerMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 推荐位：默认值、时间窗校验、上下线、删除。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallBannerServiceImpl - 推荐位")
class MallBannerServiceImplTest {

    @Mock
    private MallBannerMapper mallBannerMapper;

    private MallBannerServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallBannerServiceImpl(mallBannerMapper);
        lenient().when(mallBannerMapper.getById(anyLong()))
                .thenReturn(MallBanner.builder().id(1L).title("banner").build());
    }

    private MallBannerDTO dto() {
        MallBannerDTO dto = new MallBannerDTO();
        dto.setTitle("  首页轮播  ");
        dto.setImage("  https://x/y.png  ");
        return dto;
    }

    @Test
    @DisplayName("新增：不传的字段取默认值（position=1 / linkType=1 / sort=0 / status=1），首尾空格被去掉")
    void createAppliesDefaults() {
        service.save(dto());

        ArgumentCaptor<MallBanner> saved = ArgumentCaptor.forClass(MallBanner.class);
        verify(mallBannerMapper).insert(saved.capture());
        MallBanner b = saved.getValue();

        assertThat(b.getTitle()).isEqualTo("首页轮播");
        assertThat(b.getImage()).isEqualTo("https://x/y.png");
        assertThat(b.getPosition()).isEqualTo(1);
        assertThat(b.getLinkType()).isEqualTo(1);
        assertThat(b.getSort()).isZero();
        assertThat(b.getStatus()).isEqualTo(1);
        assertThat(b.getCreateTime()).isNotNull();
    }

    @Test
    @DisplayName("新增：失效时间早于生效时间 → 拒绝")
    void createRejectsBadTimeWindow() {
        MallBannerDTO dto = dto();
        dto.setStartTime(LocalDateTime.of(2026, 9, 20, 0, 0));
        dto.setEndTime(LocalDateTime.of(2026, 9, 19, 0, 0));

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("失效时间不能早于生效时间");
        verify(mallBannerMapper, never()).insert(any());
    }

    @Test
    @DisplayName("新增：只给一端时间（只设开始或只设结束）→ 允许")
    void createAllowsHalfOpenWindow() {
        MallBannerDTO dto = dto();
        dto.setStartTime(LocalDateTime.of(2026, 9, 18, 0, 0));
        service.save(dto);
        verify(mallBannerMapper).insert(any());
    }

    @Test
    @DisplayName("新增：时间窗相等（start == end）→ 允许")
    void createAllowsSameInstant() {
        LocalDateTime t = LocalDateTime.of(2026, 9, 18, 0, 0);
        MallBannerDTO dto = dto();
        dto.setStartTime(t);
        dto.setEndTime(t);
        service.save(dto);
        verify(mallBannerMapper).insert(any());
    }

    @Test
    @DisplayName("修改：先校验存在")
    void updateRequiresExisting() {
        when(mallBannerMapper.getById(99L)).thenReturn(null);

        MallBannerDTO dto = dto();
        dto.setId(99L);

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("推荐位不存在");
    }

    @Test
    @DisplayName("上下线：状态只能是 1 / 0")
    void statusValidation() {
        assertThatThrownBy(() -> service.updateStatus(1L, 2))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("只能是 1（上线）或 0（下线）");
        assertThatThrownBy(() -> service.updateStatus(1L, null))
                .isInstanceOf(MallBusinessException.class);

        service.updateStatus(1L, 0);
        verify(mallBannerMapper).updateStatus(1L, 0);
    }

    @Test
    @DisplayName("删除：先校验存在")
    void removeRequiresExisting() {
        when(mallBannerMapper.getById(99L)).thenReturn(null);

        assertThatThrownBy(() -> service.remove(99L))
                .isInstanceOf(MallBusinessException.class);
        verify(mallBannerMapper, never()).deleteById(anyLong());
    }

    @Test
    @DisplayName("前台列表与后台列表分别走 listEffective / list")
    void listVariants() {
        service.listEffective(1);
        verify(mallBannerMapper).listEffective(1);

        service.list(1, 0);
        verify(mallBannerMapper).list(1, 0);
    }
}
