package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallCategoryDTO;
import com.supermall.mall.entity.MallCategory;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallCategoryMapper;
import com.supermall.mall.service.MallCategoryService;
import com.supermall.mall.vo.CategoryCountVO;
import com.supermall.mall.vo.MallCategoryTreeVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 商品分类：多级（最多 3 级）。
 *
 * <p>分类总量很小（几十条），所以**一次全查出来在内存里拼树 / 算后代**，
 * 既不递归查库，也不依赖 MySQL 8+ 的递归 CTE，换库也不会挂。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallCategoryServiceImpl implements MallCategoryService {

    private static final int ROOT_PARENT_ID = 0;
    private static final int MAX_LEVEL = 3;
    private static final int ENABLED = 1;

    private final MallCategoryMapper mallCategoryMapper;

    @Override
    public List<MallCategoryTreeVO> tree(Integer status) {
        List<MallCategory> all = mallCategoryMapper.listAll(status);

        // id -> 节点
        Map<Long, MallCategoryTreeVO> nodes = new LinkedHashMap<>();
        for (MallCategory c : all) {
            nodes.put(c.getId(), toNode(c));
        }

        List<MallCategoryTreeVO> roots = new ArrayList<>();
        for (MallCategory c : all) {
            MallCategoryTreeVO node = nodes.get(c.getId());
            MallCategoryTreeVO parent = c.getParentId() == null ? null : nodes.get(c.getParentId());
            if (parent == null) {
                // parentId=0，或父分类被 status 过滤掉了 —— 都当根节点返回，
                // 否则那些分类会"凭空消失"，后台看不到也没法处理
                roots.add(node);
            } else {
                parent.getChildren().add(node);
            }
        }
        sortRecursively(roots);
        return roots;
    }

    @Override
    public List<MallCategoryTreeVO> treeWithProductCount() {
        List<MallCategoryTreeVO> roots = tree(ENABLED);

        // 一次查出各分类自己的在售商品数，再在内存里往父节点累加
        Map<Long, Integer> directCount = new HashMap<>();
        for (CategoryCountVO c : mallCategoryMapper.countOnSaleGroupByCategory()) {
            directCount.put(c.getCategoryId(), c.getTotal() == null ? 0 : c.getTotal());
        }
        fillProductCount(roots, directCount);
        return roots;
    }

    /**
     * 后序遍历累加商品数。
     *
     * @return 该层所有节点的商品数之和（供父节点累加）
     */
    private int fillProductCount(List<MallCategoryTreeVO> nodes, Map<Long, Integer> directCount) {
        int sum = 0;
        for (MallCategoryTreeVO node : nodes) {
            int self = directCount.getOrDefault(node.getId(), 0);
            int fromChildren = fillProductCount(node.getChildren(), directCount);
            node.setProductCount(self + fromChildren);
            sum += node.getProductCount();
        }
        return sum;
    }

    @Override
    public List<Long> selfAndDescendantIds(Long id) {
        getById(id);
        List<MallCategory> all = mallCategoryMapper.listAll(null);
        return collectDescendants(id, all);
    }

    @Override
    public MallCategory getById(Long id) {
        MallCategory c = mallCategoryMapper.getById(id);
        if (c == null) {
            throw new MallBusinessException(MallMessageConstant.CATEGORY_NOT_FOUND);
        }
        return c;
    }

    @Override
    @Transactional
    public Long save(MallCategoryDTO dto) {
        long parentId = dto.getParentId() == null ? ROOT_PARENT_ID : dto.getParentId();
        int level = ROOT_PARENT_ID == parentId ? 1 : resolveChildLevel(parentId);

        // 同级重名（排除自己）
        MallCategory same = mallCategoryMapper.getByParentAndName(parentId, dto.getName());
        if (same != null && !same.getId().equals(dto.getId())) {
            throw new MallBusinessException(MallMessageConstant.CATEGORY_NAME_EXISTS);
        }

        LocalDateTime now = LocalDateTime.now();
        if (dto.getId() == null) {
            MallCategory c = MallCategory.builder()
                    .parentId(parentId)
                    .level(level)
                    .name(dto.getName())
                    .icon(dto.getIcon())
                    .sort(dto.getSort() == null ? 0 : dto.getSort())
                    .status(dto.getStatus() == null ? ENABLED : dto.getStatus())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            mallCategoryMapper.insert(c);
            return c.getId();
        }

        // ---------- 修改 ----------
        MallCategory exist = getById(dto.getId());
        if (!exist.getParentId().equals(parentId)) {
            // 改父节点要连带重算整棵子树的 level，还会引入"挂到自己后代下"的环。
            // P1 不做移动分类这个功能，明确拒绝而不是留下脏数据。
            throw new MallBusinessException("暂不支持移动分类（父分类不可改），如需调整请新建后删除旧的");
        }
        mallCategoryMapper.update(MallCategory.builder()
                .id(dto.getId())
                .name(dto.getName())
                .icon(dto.getIcon())
                .sort(dto.getSort())
                .status(dto.getStatus())
                .updateTime(now)
                .build());
        return dto.getId();
    }

    @Override
    @Transactional
    public void remove(Long id) {
        getById(id);
        if (!mallCategoryMapper.listChildren(id).isEmpty()) {
            throw new MallBusinessException(MallMessageConstant.CATEGORY_HAS_CHILDREN);
        }
        if (mallCategoryMapper.countProducts(id) > 0) {
            throw new MallBusinessException(MallMessageConstant.CATEGORY_HAS_PRODUCT);
        }
        mallCategoryMapper.deleteById(id);
    }

    /** 子分类的层级 = 父层级 + 1，并卡住最大层级 */
    private int resolveChildLevel(long parentId) {
        MallCategory parent = mallCategoryMapper.getById(parentId);
        if (parent == null) {
            throw new MallBusinessException(MallMessageConstant.CATEGORY_NOT_FOUND);
        }
        int level = (parent.getLevel() == null ? 1 : parent.getLevel()) + 1;
        if (level > MAX_LEVEL) {
            throw new MallBusinessException(MallMessageConstant.CATEGORY_MAX_LEVEL);
        }
        return level;
    }

    /** 广度优先收集后代 id（含自己） */
    private List<Long> collectDescendants(Long rootId, List<MallCategory> all) {
        List<Long> result = new ArrayList<>();
        result.add(rootId);
        List<Long> frontier = new ArrayList<>(List.of(rootId));
        while (!frontier.isEmpty()) {
            List<Long> next = new ArrayList<>();
            for (MallCategory c : all) {
                if (c.getParentId() != null && frontier.contains(c.getParentId())
                        && !result.contains(c.getId())) {
                    next.add(c.getId());
                }
            }
            result.addAll(next);
            frontier = next;
        }
        return result;
    }

    private void sortRecursively(List<MallCategoryTreeVO> nodes) {
        nodes.sort(Comparator
                .comparing((MallCategoryTreeVO n) -> n.getSort() == null ? 0 : n.getSort())
                .thenComparing(MallCategoryTreeVO::getId));
        for (MallCategoryTreeVO n : nodes) {
            if (!n.getChildren().isEmpty()) {
                sortRecursively(n.getChildren());
            }
        }
    }

    private MallCategoryTreeVO toNode(MallCategory c) {
        return MallCategoryTreeVO.builder()
                .id(c.getId())
                .parentId(c.getParentId())
                .level(c.getLevel())
                .name(c.getName())
                .icon(c.getIcon())
                .sort(c.getSort())
                .status(c.getStatus())
                .createTime(c.getCreateTime())
                .children(new ArrayList<>())
                .build();
    }
}
