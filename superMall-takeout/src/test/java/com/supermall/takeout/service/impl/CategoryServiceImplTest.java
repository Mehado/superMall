package com.supermall.takeout.service.impl;

import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.CategoryDTO;
import com.supermall.takeout.entity.Category;
import com.supermall.takeout.exception.CategoryBusinessException;
import com.supermall.takeout.mapper.CategoryMapper;
import com.supermall.takeout.mapper.ProductMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 分类管理：业态校验 + 「商家 + 业态」维度判重的单元测试。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CategoryServiceImpl - 业态与判重")
class CategoryServiceImplTest {

    private static final Long MERCHANT_ID = 1L;

    @Mock
    private CategoryMapper categoryMapper;
    @Mock
    private ProductMapper productMapper;

    private CategoryServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new CategoryServiceImpl(categoryMapper, productMapper);
        BaseContext.setCurrentId(MERCHANT_ID);
    }

    @AfterEach
    void tearDown() {
        BaseContext.clear();
    }

    private CategoryDTO newCategoryDTO(Integer businessType, String name) {
        CategoryDTO dto = new CategoryDTO();
        dto.setBusinessType(businessType);
        dto.setName(name);
        dto.setSort(1);
        return dto;
    }

    @Test
    @DisplayName("非法业态：拒绝且不落库")
    void save_invalidBusinessType_isRejected() {
        assertThatThrownBy(() -> service.save(newCategoryDTO(9, "杂物")))
                .isInstanceOf(CategoryBusinessException.class);

        verify(categoryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("同业态内分类重名：拒绝")
    void save_duplicateNameWithinSameType_isRejected() {
        when(categoryMapper.countByName(MERCHANT_ID, 1, "饮品", null)).thenReturn(1);

        assertThatThrownBy(() -> service.save(newCategoryDTO(1, "饮品")))
                .isInstanceOf(CategoryBusinessException.class);

        verify(categoryMapper, never()).insert(any());
    }

    @Test
    @DisplayName("不同业态可同名：正常保存并带上业态")
    void save_sameNameDifferentType_isAllowed() {
        when(categoryMapper.countByName(anyLong(), any(), anyString(), any())).thenReturn(0);

        service.save(newCategoryDTO(2, "饮品"));

        ArgumentCaptor<Category> captor = ArgumentCaptor.forClass(Category.class);
        verify(categoryMapper).insert(captor.capture());
        assertThat(captor.getValue().getBusinessType()).isEqualTo(2);
        assertThat(captor.getValue().getMerchantId()).isEqualTo(MERCHANT_ID);
    }
}
