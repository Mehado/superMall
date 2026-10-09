package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallCategoryDTO;
import com.supermall.mall.entity.MallCategory;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallCategoryMapper;
import com.supermall.mall.vo.CategoryCountVO;
import com.supermall.mall.vo.MallCategoryTreeVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 多级分类：拼树、算后代、层级限制、删除保护、商品数累加。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallCategoryServiceImpl - 多级分类")
class MallCategoryServiceImplTest {

    @Mock
    private MallCategoryMapper mallCategoryMapper;

    private MallCategoryServiceImpl service;

    /** 1 服饰鞋包 (根) ├ 2 男装 ├ 4 T恤 / 5 衬衫  └ 3 鞋靴 └ 6 运动鞋   7 食品饮料(根) */
    private static MallCategory cat(long id, long parentId, int level, String name, int sort) {
        return MallCategory.builder().id(id).parentId(parentId).level(level).name(name).sort(sort).status(1).build();
    }

    private static final List<MallCategory> ALL = List.of(
            cat(1, 0, 1, "服饰鞋包", 1),
            cat(2, 1, 2, "男装", 1),
            cat(3, 1, 2, "鞋靴", 2),
            cat(4, 2, 3, "T恤", 1),
            cat(5, 2, 3, "衬衫", 2),
            cat(6, 3, 3, "运动鞋", 1),
            cat(7, 0, 1, "食品饮料", 2)
    );

    @BeforeEach
    void setUp() {
        service = new MallCategoryServiceImpl(mallCategoryMapper);
        lenient().when(mallCategoryMapper.listAll(any())).thenReturn(ALL);
        // save() 会先 resolveChildLevel(parentId) 查父分类是否存在，所以 getById 要能按 id 取到种子数据
        lenient().when(mallCategoryMapper.getById(anyLong())).thenAnswer(inv -> {
            Long id = inv.getArgument(0, Long.class);
            return ALL.stream().filter(c -> c.getId().equals(id)).findFirst().orElse(null);
        });
    }

    private MallCategoryTreeVO find(List<MallCategoryTreeVO> nodes, String name) {
        for (MallCategoryTreeVO n : nodes) {
            if (name.equals(n.getName())) {
                return n;
            }
            MallCategoryTreeVO hit = find(n.getChildren(), name);
            if (hit != null) {
                return hit;
            }
        }
        return null;
    }

    @Test
    @DisplayName("拼树：3 级结构正确，root 只含 parentId=0 的两个节点")
    void buildTree() {
        List<MallCategoryTreeVO> roots = service.tree(null);

        assertThat(roots).extracting(MallCategoryTreeVO::getName).containsExactly("服饰鞋包", "食品饮料");
        MallCategoryTreeVO clothing = find(roots, "服饰鞋包");
        assertThat(clothing.getChildren()).extracting(MallCategoryTreeVO::getName).containsExactly("男装", "鞋靴");
        MallCategoryTreeVO menswear = find(roots, "男装");
        assertThat(menswear.getChildren()).extracting(MallCategoryTreeVO::getName).containsExactly("T恤", "衬衫");
        assertThat(find(roots, "T恤").getChildren()).isEmpty();
    }

    @Test
    @DisplayName("拼树：父分类被 status 过滤掉时，子分类要提升为根节点（否则会凭空消失）")
    void orphanBecomesRoot() {
        // 只返回 level>=2 的，父节点不在结果里
        when(mallCategoryMapper.listAll(any())).thenReturn(List.of(cat(4, 2, 3, "T恤", 1)));

        List<MallCategoryTreeVO> roots = service.tree(1);

        assertThat(roots).extracting(MallCategoryTreeVO::getName).containsExactly("T恤");
    }

    @Test
    @DisplayName("排序：同级按 sort 升序，再按 id")
    void treeSorted() {
        when(mallCategoryMapper.listAll(any())).thenReturn(List.of(
                cat(20, 0, 1, "后加的", 9),
                cat(1, 0, 1, "服饰鞋包", 1)
        ));
        List<MallCategoryTreeVO> roots = service.tree(null);
        assertThat(roots).extracting(MallCategoryTreeVO::getName).containsExactly("服饰鞋包", "后加的");
    }

    @Test
    @DisplayName("后代 id：含自己 + 所有层级后代")
    void descendantIds() {
        when(mallCategoryMapper.getById(1L)).thenReturn(ALL.get(0));

        List<Long> ids = service.selfAndDescendantIds(1L);

        assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L, 4L, 5L, 6L);
        assertThat(ids).doesNotContain(7L);
    }

    @Test
    @DisplayName("新增：子分类层级 = 父层级 + 1")
    void createChildComputesLevel() {
        when(mallCategoryMapper.getById(2L)).thenReturn(ALL.get(1)); // 男装 level=2
        when(mallCategoryMapper.getByParentAndName(anyLong(), any())).thenReturn(null);

        MallCategoryDTO dto = new MallCategoryDTO();
        dto.setParentId(2L);
        dto.setName("外套");

        service.save(dto);

        ArgumentCaptor<MallCategory> saved = ArgumentCaptor.forClass(MallCategory.class);
        verify(mallCategoryMapper).insert(saved.capture());
        assertThat(saved.getValue().getLevel()).isEqualTo(3);
        assertThat(saved.getValue().getParentId()).isEqualTo(2L);
    }

    @Test
    @DisplayName("新增：parentId 不传 = 顶级，level = 1")
    void createRoot() {
        when(mallCategoryMapper.getByParentAndName(anyLong(), any())).thenReturn(null);

        MallCategoryDTO dto = new MallCategoryDTO();
        dto.setName("新顶级");

        service.save(dto);

        ArgumentCaptor<MallCategory> saved = ArgumentCaptor.forClass(MallCategory.class);
        verify(mallCategoryMapper).insert(saved.capture());
        assertThat(saved.getValue().getLevel()).isEqualTo(1);
        assertThat(saved.getValue().getParentId()).isZero();
    }

    @Test
    @DisplayName("新增：往第 3 级下再挂一层（第 4 级）→ 拒绝")
    void cannotExceedMaxLevel() {
        when(mallCategoryMapper.getById(4L)).thenReturn(ALL.get(3)); // T恤 level=3

        MallCategoryDTO dto = new MallCategoryDTO();
        dto.setParentId(4L);
        dto.setName("超四级");

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CATEGORY_MAX_LEVEL);
    }

    @Test
    @DisplayName("新增：同级重名 → 拒绝")
    void duplicateNameInSameParent() {
        when(mallCategoryMapper.getByParentAndName(2L, "T恤")).thenReturn(ALL.get(3));

        MallCategoryDTO dto = new MallCategoryDTO();
        dto.setParentId(2L);
        dto.setName("T恤");

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CATEGORY_NAME_EXISTS);
    }

    @Test
    @DisplayName("修改：不允许改父分类（要重算子树 level，还会引入环）")
    void moveNotSupported() {
        when(mallCategoryMapper.getById(4L)).thenReturn(ALL.get(3)); // T恤 parentId=2
        when(mallCategoryMapper.getByParentAndName(3L, "T恤")).thenReturn(null);

        MallCategoryDTO dto = new MallCategoryDTO();
        dto.setId(4L);
        dto.setParentId(3L);   // 想挪到「鞋靴」下
        dto.setName("T恤");

        assertThatThrownBy(() -> service.save(dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessageContaining("暂不支持移动分类");
    }

    @Test
    @DisplayName("修改：同名改回自己不算重名（允许保存）")
    void updateSelfNameOk() {
        when(mallCategoryMapper.getById(4L)).thenReturn(ALL.get(3));
        when(mallCategoryMapper.getByParentAndName(2L, "T恤")).thenReturn(ALL.get(3)); // 查到的是自己

        MallCategoryDTO dto = new MallCategoryDTO();
        dto.setId(4L);
        dto.setParentId(2L);
        dto.setName("T恤");
        dto.setSort(5);

        service.save(dto);

        verify(mallCategoryMapper).update(any());
    }

    @Test
    @DisplayName("删除：有子分类 → 拒绝")
    void deleteWithChildrenRejected() {
        when(mallCategoryMapper.getById(2L)).thenReturn(ALL.get(1));
        when(mallCategoryMapper.listChildren(2L)).thenReturn(List.of(ALL.get(3), ALL.get(4)));

        assertThatThrownBy(() -> service.remove(2L))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CATEGORY_HAS_CHILDREN);
    }

    @Test
    @DisplayName("删除：叶子但有商品 → 拒绝")
    void deleteWithProductRejected() {
        when(mallCategoryMapper.getById(4L)).thenReturn(ALL.get(3));
        when(mallCategoryMapper.listChildren(4L)).thenReturn(List.of());
        when(mallCategoryMapper.countProducts(4L)).thenReturn(3);

        assertThatThrownBy(() -> service.remove(4L))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.CATEGORY_HAS_PRODUCT);
    }

    @Test
    @DisplayName("删除：空叶子 → 放行")
    void deleteEmptyLeaf() {
        when(mallCategoryMapper.getById(4L)).thenReturn(ALL.get(3));
        when(mallCategoryMapper.listChildren(4L)).thenReturn(List.of());
        when(mallCategoryMapper.countProducts(4L)).thenReturn(0);

        service.remove(4L);

        verify(mallCategoryMapper).deleteById(4L);
    }

    // ------------------------------------------------------------
    // P2：分类导航的商品数
    // ------------------------------------------------------------

    @Test
    @DisplayName("商品数：父节点 = 自己 + 所有后代，逐层累加")
    void productCountAccumulatesUpward() {
        // T恤 3 个、衬衫 2 个、运动鞋 5 个；男装/鞋靴/服饰鞋包 自己没有直属商品
        when(mallCategoryMapper.countOnSaleGroupByCategory()).thenReturn(List.of(
                CategoryCountVO.builder().categoryId(4L).total(3).build(),
                CategoryCountVO.builder().categoryId(5L).total(2).build(),
                CategoryCountVO.builder().categoryId(6L).total(5).build()
        ));

        List<MallCategoryTreeVO> roots = service.treeWithProductCount();

        assertThat(find(roots, "T恤").getProductCount()).isEqualTo(3);
        assertThat(find(roots, "衬衫").getProductCount()).isEqualTo(2);
        assertThat(find(roots, "男装").getProductCount()).isEqualTo(5);      // 3 + 2
        assertThat(find(roots, "鞋靴").getProductCount()).isEqualTo(5);
        assertThat(find(roots, "服饰鞋包").getProductCount()).isEqualTo(10); // 5 + 5
        assertThat(find(roots, "食品饮料").getProductCount()).isZero();      // 没有商品
    }

    @Test
    @DisplayName("商品数：父分类自己也有商品时要一起算进去")
    void productCountIncludesSelf() {
        when(mallCategoryMapper.countOnSaleGroupByCategory()).thenReturn(List.of(
                CategoryCountVO.builder().categoryId(2L).total(1).build(),  // 男装自己 1 个
                CategoryCountVO.builder().categoryId(4L).total(3).build()
        ));

        List<MallCategoryTreeVO> roots = service.treeWithProductCount();

        assertThat(find(roots, "男装").getProductCount()).isEqualTo(4);       // 1 + 3
        assertThat(find(roots, "服饰鞋包").getProductCount()).isEqualTo(4);
    }

    @Test
    @DisplayName("商品数：后台的 tree() 不填 productCount（省一次聚合查询）")
    void backendTreeHasNoCount() {
        List<MallCategoryTreeVO> roots = service.tree(null);

        assertThat(find(roots, "服饰鞋包").getProductCount()).isNull();
        assertThat(find(roots, "T恤").getProductCount()).isNull();
        // 不应该去查商品数
        verify(mallCategoryMapper, never()).countOnSaleGroupByCategory();
    }
}
