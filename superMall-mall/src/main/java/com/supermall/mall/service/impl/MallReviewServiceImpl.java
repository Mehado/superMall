package com.supermall.mall.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallReviewFollowDTO;
import com.supermall.mall.dto.MallReviewItemDTO;
import com.supermall.mall.dto.MallReviewSubmitDTO;
import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.entity.MallOrderItem;
import com.supermall.mall.entity.MallReview;
import com.supermall.mall.entity.MallReviewImage;
import com.supermall.mall.enumeration.MallOrderStatus;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallOrderItemMapper;
import com.supermall.mall.mapper.MallOrderMapper;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.mapper.MallReviewImageMapper;
import com.supermall.mall.mapper.MallReviewMapper;
import com.supermall.mall.service.MallReviewService;
import com.supermall.mall.vo.MallOrderReviewVO;
import com.supermall.mall.vo.MallReviewStatsVO;
import com.supermall.mall.vo.MallReviewVO;
import com.supermall.result.PageResult;
import com.supermall.utils.RichTextSanitizer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 评价服务实现。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class MallReviewServiceImpl implements MallReviewService {

    private static final int TYPE_INITIAL = 1;
    private static final int TYPE_FOLLOW = 2;
    private static final int MAX_IMAGES = 9;
    private static final BigDecimal DEFAULT_RATING = new BigDecimal("5.00");
    private static final BigDecimal DEFAULT_GOOD_RATE = new BigDecimal("100.00");

    private final MallReviewMapper mallReviewMapper;
    private final MallReviewImageMapper mallReviewImageMapper;
    private final MallOrderMapper mallOrderMapper;
    private final MallOrderItemMapper mallOrderItemMapper;
    private final MallProductMapper mallProductMapper;

    // ============================================================
    // 买家：发表 / 追评
    // ============================================================

    @Override
    @Transactional
    public int submit(Long userId, MallReviewSubmitDTO dto) {
        MallOrder order = mallOrderMapper.getByNumber(dto.getOrderNumber());
        if (order == null || !userId.equals(order.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.ORDER_NOT_FOUND);
        }
        if (!MallOrderStatus.COMPLETED.name().equals(order.getStatus())) {
            throw new MallBusinessException(MallMessageConstant.REVIEW_NOT_ALLOWED);
        }

        // 明细必须在订单里
        Map<Long, MallOrderItem> itemMap = new LinkedHashMap<>();
        for (MallOrderItem item : mallOrderItemMapper.listByOrderId(order.getId())) {
            itemMap.put(item.getId(), item);
        }
        // 同一请求里不能重复提交同一个明细
        Set<Long> seen = new HashSet<>();
        for (MallReviewItemDTO r : dto.getReviews()) {
            if (!seen.add(r.getOrderItemId())) {
                throw new MallBusinessException(MallMessageConstant.REVIEW_DUPLICATE_ITEM);
            }
        }

        LocalDateTime now = LocalDateTime.now();
        Set<Long> touchedProducts = new LinkedHashSet<>();
        for (MallReviewItemDTO r : dto.getReviews()) {
            MallOrderItem item = itemMap.get(r.getOrderItemId());
            if (item == null) {
                throw new MallBusinessException(MallMessageConstant.REVIEW_ITEM_NOT_IN_ORDER);
            }
            if (mallReviewMapper.getByOrderItemId(r.getOrderItemId()) != null) {
                throw new MallBusinessException(MallMessageConstant.REVIEW_ALREADY);
            }
            List<String> images = cleanImages(r.getImages());

            MallReview review = MallReview.builder()
                    .orderId(order.getId())
                    .orderNumber(order.getOrderNumber())
                    .orderItemId(item.getId())
                    .userId(userId)
                    .productId(item.getProductId())
                    .skuId(item.getSkuId())
                    .rating(r.getRating())
                    // 评价是纯文本：剥掉标签（内容保留），不转义，避免前端二次转义露出 &lt;
                    .content(RichTextSanitizer.plainText(trimToNull(r.getContent())))
                    .anonymous(Boolean.TRUE.equals(r.getAnonymous()) ? 1 : 0)
                    .createTime(now)
                    .updateTime(now)
                    .build();
            mallReviewMapper.insert(review);
            insertImages(review.getId(), images, TYPE_INITIAL, now);
            touchedProducts.add(item.getProductId());
        }

        // 重算商品评价聚合
        for (Long productId : touchedProducts) {
            mallProductMapper.refreshReviewAggregates(productId);
        }
        log.info("买家评价：orderNumber={}, userId={}, 评价 {} 条",
                dto.getOrderNumber(), userId, dto.getReviews().size());
        return dto.getReviews().size();
    }

    @Override
    @Transactional
    public void follow(Long userId, Long reviewId, MallReviewFollowDTO dto) {
        MallReview review = mallReviewMapper.getById(reviewId);
        if (review == null || !userId.equals(review.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.REVIEW_NOT_FOUND);
        }
        LocalDateTime now = LocalDateTime.now();
        int rows = mallReviewMapper.updateFollow(
                reviewId, RichTextSanitizer.plainText(dto.getContent().trim()), now);
        if (rows == 0) {
            throw new MallBusinessException(MallMessageConstant.REVIEW_ALREADY_FOLLOWED);
        }
        insertImages(reviewId, cleanImages(dto.getImages()), TYPE_FOLLOW, now);
        // 追评不改变评分，不需要重算聚合
        log.info("买家追评：reviewId={}, userId={}", reviewId, userId);
    }

    // ============================================================
    // 买家：我的评价 / 订单评价情况
    // ============================================================

    @Override
    public PageResult pageMine(Long userId, Integer page, Integer pageSize) {
        PageHelper.startPage(safePage(page), safeSize(pageSize));
        Page<MallReviewVO> p = mallReviewMapper.pageByUser(userId);
        assemble(p.getResult());
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public MallOrderReviewVO orderReviews(Long userId, String orderNumber) {
        MallOrder order = mallOrderMapper.getByNumber(orderNumber);
        if (order == null || !userId.equals(order.getUserId())) {
            throw new MallBusinessException(MallMessageConstant.ORDER_NOT_FOUND);
        }
        List<MallOrderItem> items = mallOrderItemMapper.listByOrderId(order.getId());
        List<Long> itemIds = items.stream().map(MallOrderItem::getId).toList();

        Map<Long, MallReview> reviewByItem = new HashMap<>();
        if (!itemIds.isEmpty()) {
            for (MallReview r : mallReviewMapper.listByOrderItemIds(itemIds)) {
                reviewByItem.put(r.getOrderItemId(), r);
            }
        }
        // 批量加载图片
        Map<Long, List<MallReviewImage>> imagesByReview = loadImages(
                reviewByItem.values().stream().map(MallReview::getId).toList());

        List<MallOrderReviewVO.OrderItemReviewVO> vos = new ArrayList<>();
        for (MallOrderItem item : items) {
            MallReview r = reviewByItem.get(item.getId());
            vos.add(MallOrderReviewVO.OrderItemReviewVO.builder()
                    .orderItemId(item.getId())
                    .productId(item.getProductId())
                    .skuId(item.getSkuId())
                    .productName(item.getProductName())
                    .productImage(item.getImage())
                    .specText(item.getSpecText())
                    .review(r == null ? null : toVOFromEntity(r, imagesByReview.getOrDefault(r.getId(), List.of())))
                    .build());
        }
        return MallOrderReviewVO.builder()
                .orderNumber(orderNumber)
                .orderStatus(order.getStatus())
                .canReview(MallOrderStatus.COMPLETED.name().equals(order.getStatus()))
                .items(vos)
                .build();
    }

    // ============================================================
    // 公开：商品评价
    // ============================================================

    @Override
    public PageResult pageByProduct(Long productId, Integer rating, Boolean hasImage,
                                    Integer page, Integer pageSize) {
        PageHelper.startPage(safePage(page), safeSize(pageSize));
        Page<MallReviewVO> p = mallReviewMapper.pageByProduct(productId, rating, hasImage);
        assemble(p.getResult());
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    public MallReviewStatsVO stats(Long productId) {
        int count = mallReviewMapper.countByProduct(productId);
        BigDecimal avg = mallReviewMapper.avgRatingByProduct(productId);
        int good = mallReviewMapper.countGoodByProduct(productId);

        BigDecimal rating = avg == null ? DEFAULT_RATING : avg;
        BigDecimal goodRate = count == 0
                ? DEFAULT_GOOD_RATE
                : BigDecimal.valueOf(good * 100.0 / count).setScale(2, RoundingMode.HALF_UP);

        Map<Integer, Integer> distMap = new HashMap<>();
        for (MallReviewStatsVO.StarCount sc : mallReviewMapper.distributionByProduct(productId)) {
            distMap.put(sc.getStar(), sc.getCount());
        }
        List<MallReviewStatsVO.StarCount> distribution = new ArrayList<>();
        for (int star = 5; star >= 1; star--) {
            distribution.add(MallReviewStatsVO.StarCount.builder()
                    .star(star)
                    .count(distMap.getOrDefault(star, 0))
                    .build());
        }
        return MallReviewStatsVO.builder()
                .rating(rating)
                .reviewCount(count)
                .goodRate(goodRate)
                .distribution(distribution)
                .build();
    }

    // ============================================================
    // 运营后台
    // ============================================================

    @Override
    public PageResult pageByAdmin(Long productId, Long userId, Integer rating,
                                  Integer page, Integer pageSize) {
        PageHelper.startPage(safePage(page), safeSize(pageSize));
        Page<MallReviewVO> p = mallReviewMapper.pageByAdmin(productId, userId, rating);
        assemble(p.getResult());
        return new PageResult(p.getTotal(), p.getResult());
    }

    @Override
    @Transactional
    public void delete(Long reviewId) {
        MallReview review = mallReviewMapper.getById(reviewId);
        if (review == null) {
            throw new MallBusinessException(MallMessageConstant.REVIEW_NOT_FOUND);
        }
        mallReviewImageMapper.deleteByReviewId(reviewId);
        mallReviewMapper.deleteById(reviewId);
        mallProductMapper.refreshReviewAggregates(review.getProductId());
        log.info("后台删除评价：reviewId={}, productId={}", reviewId, review.getProductId());
    }

    // ============================================================
    // 私有：组装
    // ============================================================

    /** 给评价 VO 批量补图片 + 处理匿名昵称 */
    private void assemble(List<MallReviewVO> list) {
        if (list == null || list.isEmpty()) {
            return;
        }
        Map<Long, List<MallReviewImage>> imagesByReview = loadImages(
                list.stream().map(MallReviewVO::getId).toList());
        for (MallReviewVO vo : list) {
            List<MallReviewImage> imgs = imagesByReview.getOrDefault(vo.getId(), List.of());
            vo.setImages(imgs.stream().filter(i -> TYPE_INITIAL == safeType(i)).map(MallReviewImage::getUrl).toList());
            vo.setFollowImages(imgs.stream().filter(i -> TYPE_FOLLOW == safeType(i)).map(MallReviewImage::getUrl).toList());
            vo.setNickname(displayName(vo.getAnonymous(), vo.getNickname()));
        }
    }

    private Map<Long, List<MallReviewImage>> loadImages(List<Long> reviewIds) {
        if (reviewIds == null || reviewIds.isEmpty()) {
            return Map.of();
        }
        Map<Long, List<MallReviewImage>> map = new HashMap<>();
        for (MallReviewImage img : mallReviewImageMapper.listByReviewIds(reviewIds)) {
            map.computeIfAbsent(img.getReviewId(), k -> new ArrayList<>()).add(img);
        }
        return map;
    }

    private MallReviewVO toVOFromEntity(MallReview r, List<MallReviewImage> images) {
        return MallReviewVO.builder()
                .id(r.getId())
                .productId(r.getProductId())
                .skuId(r.getSkuId())
                .orderItemId(r.getOrderItemId())
                .rating(r.getRating())
                .content(r.getContent())
                .images(images.stream().filter(i -> TYPE_INITIAL == safeType(i)).map(MallReviewImage::getUrl).toList())
                .followContent(r.getFollowContent())
                .followImages(images.stream().filter(i -> TYPE_FOLLOW == safeType(i)).map(MallReviewImage::getUrl).toList())
                .followTime(r.getFollowTime())
                .anonymous(Integer.valueOf(1).equals(r.getAnonymous()))
                .createTime(r.getCreateTime())
                .build();
    }

    /**
     * 匿名 -> 「匿名用户」；实名时用昵称，昵称为空则用邮箱并打码。
     */
    private String displayName(Boolean anonymous, String raw) {
        if (Boolean.TRUE.equals(anonymous) || raw == null || raw.isBlank()) {
            return "匿名用户";
        }
        if (raw.contains("@")) {
            int at = raw.indexOf('@');
            String local = raw.substring(0, at);
            String first = local.isEmpty() ? "*" : local.substring(0, 1);
            return first + "***" + raw.substring(at);
        }
        return raw;
    }

    private void insertImages(Long reviewId, List<String> urls, int type, LocalDateTime now) {
        if (urls == null || urls.isEmpty()) {
            return;
        }
        List<MallReviewImage> images = new ArrayList<>();
        for (int i = 0; i < urls.size(); i++) {
            images.add(MallReviewImage.builder()
                    .reviewId(reviewId)
                    .url(urls.get(i))
                    .type(type)
                    .sort(i)
                    .createTime(now)
                    .build());
        }
        mallReviewImageMapper.batchInsert(images);
    }

    private List<String> cleanImages(List<String> images) {
        if (images == null) {
            return List.of();
        }
        List<String> cleaned = new ArrayList<>();
        for (String url : images) {
            if (url != null && !url.isBlank()) {
                cleaned.add(url.trim());
            }
        }
        if (cleaned.size() > MAX_IMAGES) {
            throw new MallBusinessException(MallMessageConstant.REVIEW_TOO_MANY_IMAGES);
        }
        return cleaned;
    }

    private int safeType(MallReviewImage img) {
        return img.getType() == null ? TYPE_INITIAL : img.getType();
    }

    private String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private int safePage(Integer page) {
        return page == null || page < 1 ? 1 : page;
    }

    private int safeSize(Integer pageSize) {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }
}
