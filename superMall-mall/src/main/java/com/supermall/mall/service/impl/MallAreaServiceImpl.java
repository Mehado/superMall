package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.service.MallAreaService;
import com.supermall.mall.vo.MallAreaResolvedVO;
import com.supermall.mall.vo.MallAreaVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import tools.jackson.databind.json.JsonMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 区划数据在内存里的实现。
 *
 * <p>加载一次、只读、无锁：{@code byCode} 与 {@code childrenByParent} 在构造器里建好后
 * 全程不再修改，所以并发读是安全的。
 */
@Service
@Slf4j
public class MallAreaServiceImpl implements MallAreaService {

    /** mall-area.json 的一行。record 让 Jackson 直接反序列化。 */
    private record AreaNode(String code, String name, String parentCode, Integer level) {
    }

    private static final String RESOURCE = "mall-area.json";

    private final Map<String, AreaNode> byCode;
    private final Map<String, List<MallAreaVO>> childrenByParent;

    public MallAreaServiceImpl() {
        Map<String, AreaNode> codeIndex = new HashMap<>();
        Map<String, List<MallAreaVO>> children = new LinkedHashMap<>();
        try (InputStream in = new ClassPathResource(RESOURCE).getInputStream()) {
            AreaNode[] nodes = JsonMapper.builder().build().readValue(in, AreaNode[].class);
            for (AreaNode n : nodes) {
                codeIndex.put(n.code(), n);
            }
            // 保持文件里的顺序（同省内按 code 升序），前端下拉才是稳定的
            for (AreaNode n : nodes) {
                children.computeIfAbsent(n.parentCode(), k -> new ArrayList<>())
                        .add(MallAreaVO.builder()
                                .code(n.code())
                                .name(n.name())
                                .level(n.level())
                                .hasChildren(false)
                                .build());
            }
            // 回填 hasChildren：能在 children 里当 key 的，说明下面还有一级
            for (List<MallAreaVO> list : children.values()) {
                for (MallAreaVO vo : list) {
                    vo.setHasChildren(children.containsKey(vo.getCode()));
                }
            }
        } catch (Exception e) {
            // 静态数据加载失败属于启动期致命错误，直接让 Bean 创建失败
            throw new IllegalStateException("加载行政区划数据失败：" + RESOURCE, e);
        }
        this.byCode = Map.copyOf(codeIndex);
        this.childrenByParent = Map.copyOf(children);
        log.info("[mall] 行政区划数据加载完成：{} 个节点", byCode.size());
    }

    @Override
    public List<MallAreaVO> children(String parentCode) {
        String parent = (parentCode == null || parentCode.isBlank()) ? ROOT_CODE : parentCode.trim();
        return childrenByParent.getOrDefault(parent, List.of());
    }

    @Override
    public MallAreaResolvedVO resolve(String areaCode) {
        if (areaCode == null || areaCode.isBlank()) {
            throw new MallBusinessException(MallMessageConstant.AREA_NOT_FOUND);
        }
        String code = areaCode.trim();
        AreaNode node = byCode.get(code);
        if (node == null) {
            throw new MallBusinessException(MallMessageConstant.AREA_NOT_FOUND);
        }
        // 必须是末级：选了「北京市」还没选到区县，不该能存地址
        if (childrenByParent.containsKey(code)) {
            throw new MallBusinessException(MallMessageConstant.AREA_NOT_LEAF);
        }

        // 顺着 parentCode 往上走到根，得到 省 → 市 → 区 链
        List<AreaNode> chain = new ArrayList<>();
        AreaNode cur = node;
        while (cur != null && !ROOT_CODE.equals(cur.code())) {
            chain.add(0, cur);
            cur = byCode.get(cur.parentCode());
        }

        String province = null;
        String city = null;
        String district = null;
        for (AreaNode a : chain) {
            switch (a.level() == null ? 0 : a.level()) {
                case 1 -> province = a.name();
                case 2 -> city = a.name();
                case 3 -> district = a.name();
                default -> { /* 理论上不会出现 */ }
            }
        }

        StringBuilder full = new StringBuilder();
        for (String s : new String[]{province, city, district}) {
            if (s != null && !s.isBlank()) {
                full.append(s).append(' ');
            }
        }
        return MallAreaResolvedVO.builder()
                .areaCode(code)
                .province(province)
                .city(city)
                .district(district)
                .fullName(full.toString().trim())
                .build();
    }

    @Override
    public boolean exists(String code) {
        return code != null && byCode.containsKey(code.trim());
    }
}
