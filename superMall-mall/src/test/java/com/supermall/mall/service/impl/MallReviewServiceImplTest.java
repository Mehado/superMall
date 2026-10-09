package com.supermall.mall.service.impl;

import com.supermall.mall.constant.MallMessageConstant;
import com.supermall.mall.dto.MallReviewFollowDTO;
import com.supermall.mall.dto.MallReviewItemDTO;
import com.supermall.mall.dto.MallReviewSubmitDTO;
import com.supermall.mall.entity.MallOrder;
import com.supermall.mall.entity.MallOrderItem;
import com.supermall.mall.entity.MallReview;
import com.supermall.mall.enumeration.MallOrderStatus;
import com.supermall.mall.exception.MallBusinessException;
import com.supermall.mall.mapper.MallOrderItemMapper;
import com.supermall.mall.mapper.MallOrderMapper;
import com.supermall.mall.mapper.MallProductMapper;
import com.supermall.mall.mapper.MallReviewImageMapper;
import com.supermall.mall.mapper.MallReviewMapper;
import com.supermall.mall.vo.MallReviewStatsVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 评价：仅已完成订单可评、明细粒度去重、追评一次、聚合重算、匿名。
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("MallReviewServiceImpl - 评价")
class MallReviewServiceImplTest {

    private static final Long USER = 7L;
    private static final String ORDER_NO = "20260921120000001123";

    @Mock private MallReviewMapper reviewMapper;
    @Mock private MallReviewImageMapper reviewImageMapper;
    @Mock private MallOrderMapper orderMapper;
    @Mock private MallOrderItemMapper orderItemMapper;
    @Mock private MallProductMapper productMapper;

    private MallReviewServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MallReviewServiceImpl(reviewMapper, reviewImageMapper, orderMapper,
                orderItemMapper, productMapper);
        lenient().doAnswer(inv -> {
            inv.getArgument(0, MallReview.class).setId(500L);
            return null;
        }).when(reviewMapper).insert(any(MallReview.class));
    }

    private MallOrder order(String status) {
        return MallOrder.builder().id(9L).orderNumber(ORDER_NO).userId(USER).status(status).build();
    }

    private MallOrderItem item(long itemId, long productId, long skuId) {
        return MallOrderItem.builder()
                .id(itemId).orderId(9L).productId(productId).skuId(skuId)
                .productName("T恤").specText("白 / M").build();
    }

    private MallReviewSubmitDTO submitDTO() {
        MallReviewItemDTO i1 = new MallReviewItemDTO();
        i1.setOrderItemId(100L);
        i1.setRating(5);
        i1.setContent("很好");
        i1.setImages(List.of("a.png", "b.png"));
        i1.setAnonymous(false);

        MallReviewItemDTO i2 = new MallReviewItemDTO();
        i2.setOrderItemId(101L);
        i2.setRating(3);
        i2.setAnonymous(true);

        MallReviewSubmitDTO dto = new MallReviewSubmitDTO();
        dto.setOrderNumber(ORDER_NO);
        dto.setReviews(List.of(i1, i2));
        return dto;
    }

    // ---------------- 提交 ----------------

    @Test
    @DisplayName("提交：订单不属于自己 → 订单不存在")
    void submitNotOwned() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(MallOrder.builder()
                .id(9L).orderNumber(ORDER_NO).userId(999L).status(MallOrderStatus.COMPLETED.name()).build());
        assertThatThrownBy(() -> service.submit(USER, submitDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.ORDER_NOT_FOUND);
    }

    @Test
    @DisplayName("提交：订单未完成 → 拒绝")
    void submitNotCompleted() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(order(MallOrderStatus.PENDING_RECEIPT.name()));
        assertThatThrownBy(() -> service.submit(USER, submitDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REVIEW_NOT_ALLOWED);
        verify(reviewMapper, never()).insert(any());
    }

    @Test
    @DisplayName("提交：明细不属于该订单 → 拒绝")
    void submitItemNotInOrder() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(order(MallOrderStatus.COMPLETED.name()));
        when(orderItemMapper.listByOrderId(9L)).thenReturn(List.of(item(100L, 1L, 1L)));

        assertThatThrownBy(() -> service.submit(USER, submitDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REVIEW_ITEM_NOT_IN_ORDER);
    }

    @Test
    @DisplayName("提交：明细已评过 → 拒绝")
    void submitAlreadyReviewed() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(order(MallOrderStatus.COMPLETED.name()));
        when(orderItemMapper.listByOrderId(9L)).thenReturn(List.of(item(100L, 1L, 1L), item(101L, 2L, 2L)));
        when(reviewMapper.getByOrderItemId(100L)).thenReturn(MallReview.builder().id(1L).build());

        assertThatThrownBy(() -> service.submit(USER, submitDTO()))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REVIEW_ALREADY);
    }

    @Test
    @DisplayName("提交：同一请求重复明细 → 拒绝")
    void submitDuplicateInRequest() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(order(MallOrderStatus.COMPLETED.name()));
        MallReviewSubmitDTO dto = submitDTO();
        dto.getReviews().get(1).setOrderItemId(100L);

        assertThatThrownBy(() -> service.submit(USER, dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REVIEW_DUPLICATE_ITEM);
    }

    @Test
    @DisplayName("提交成功：写评价 + 初评图，并按商品去重后重算聚合")
    void submitSuccess() {
        when(orderMapper.getByNumber(ORDER_NO)).thenReturn(order(MallOrderStatus.COMPLETED.name()));
        when(orderItemMapper.listByOrderId(9L)).thenReturn(List.of(item(100L, 1L, 1L), item(101L, 1L, 2L)));
        when(reviewMapper.getByOrderItemId(anyLong())).thenReturn(null);

        int count = service.submit(USER, submitDTO());

        assertThat(count).isEqualTo(2);
        verify(reviewMapper, times(2)).insert(any());
        // 两条评价都属于商品 1 → 聚合只重算一次
        verify(productMapper, times(1)).refreshReviewAggregates(1L);
        // 第一条有 2 张初评图；第二条没图 → 只调用一次 batchInsert
        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.supermall.mall.entity.MallReviewImage>> cap = ArgumentCaptor.forClass(List.class);
        verify(reviewImageMapper, times(1)).batchInsert(cap.capture());
        assertThat(cap.getValue()).hasSize(2);
        assertThat(cap.getValue().get(0).getType()).isEqualTo(1);
    }

    // ---------------- 追评 ----------------

    @Test
    @DisplayName("追评：不是自己的评价 → 评价不存在")
    void followNotOwned() {
        when(reviewMapper.getById(500L)).thenReturn(MallReview.builder().id(500L).userId(999L).build());
        MallReviewFollowDTO dto = new MallReviewFollowDTO();
        dto.setContent("追加");
        assertThatThrownBy(() -> service.follow(USER, 500L, dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REVIEW_NOT_FOUND);
    }

    @Test
    @DisplayName("追评：已追评过（条件更新 0 行）→ 拒绝")
    void followAlreadyDone() {
        when(reviewMapper.getById(500L)).thenReturn(MallReview.builder().id(500L).userId(USER).build());
        when(reviewMapper.updateFollow(eq(500L), any(), any())).thenReturn(0);
        MallReviewFollowDTO dto = new MallReviewFollowDTO();
        dto.setContent("追加");
        assertThatThrownBy(() -> service.follow(USER, 500L, dto))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REVIEW_ALREADY_FOLLOWED);
    }

    @Test
    @DisplayName("追评成功：写 type=2 的图片，且不动商品聚合")
    void followSuccess() {
        when(reviewMapper.getById(500L)).thenReturn(MallReview.builder().id(500L).userId(USER).productId(1L).build());
        when(reviewMapper.updateFollow(eq(500L), eq("追加"), any())).thenReturn(1);
        MallReviewFollowDTO dto = new MallReviewFollowDTO();
        dto.setContent("追加");
        dto.setImages(List.of("f.png"));

        service.follow(USER, 500L, dto);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<com.supermall.mall.entity.MallReviewImage>> cap = ArgumentCaptor.forClass(List.class);
        verify(reviewImageMapper).batchInsert(cap.capture());
        assertThat(cap.getValue().get(0).getType()).isEqualTo(2);
        verify(productMapper, never()).refreshReviewAggregates(anyLong());
    }

    // ---------------- 统计 ----------------

    @Test
    @DisplayName("统计：无评价 → 5.00 / 0 / 100.00")
    void statsEmpty() {
        when(reviewMapper.countByProduct(1L)).thenReturn(0);
        when(reviewMapper.avgRatingByProduct(1L)).thenReturn(null);
        when(reviewMapper.countGoodByProduct(1L)).thenReturn(0);
        when(reviewMapper.distributionByProduct(1L)).thenReturn(List.of());

        MallReviewStatsVO vo = service.stats(1L);

        assertThat(vo.getRating()).isEqualByComparingTo("5.00");
        assertThat(vo.getReviewCount()).isZero();
        assertThat(vo.getGoodRate()).isEqualByComparingTo("100.00");
        assertThat(vo.getDistribution()).hasSize(5);
        assertThat(vo.getDistribution().get(0).getStar()).isEqualTo(5);
        assertThat(vo.getDistribution()).allMatch(s -> s.getCount() == 0);
    }

    @Test
    @DisplayName("统计：好评率 = 4 星及以上占比，星级分布补齐 5→1")
    void statsWithData() {
        when(reviewMapper.countByProduct(1L)).thenReturn(4);
        when(reviewMapper.avgRatingByProduct(1L)).thenReturn(new BigDecimal("4.25"));
        when(reviewMapper.countGoodByProduct(1L)).thenReturn(3);
        when(reviewMapper.distributionByProduct(1L)).thenReturn(List.of(
                MallReviewStatsVO.StarCount.builder().star(5).count(2).build(),
                MallReviewStatsVO.StarCount.builder().star(3).count(1).build(),
                MallReviewStatsVO.StarCount.builder().star(1).count(1).build()));

        MallReviewStatsVO vo = service.stats(1L);

        assertThat(vo.getRating()).isEqualByComparingTo("4.25");
        assertThat(vo.getReviewCount()).isEqualTo(4);
        assertThat(vo.getGoodRate()).isEqualByComparingTo("75.00");
        assertThat(vo.getDistribution()).extracting(MallReviewStatsVO.StarCount::getStar)
                .containsExactly(5, 4, 3, 2, 1);
        assertThat(vo.getDistribution().get(1).getCount()).isZero();  // 4 星缺省补 0
        assertThat(vo.getDistribution().get(2).getCount()).isEqualTo(1);
    }

    // ---------------- 删除 ----------------

    @Test
    @DisplayName("后台删除评价：删图 + 删评价 + 重算聚合")
    void deleteSuccess() {
        when(reviewMapper.getById(500L)).thenReturn(MallReview.builder().id(500L).productId(1L).build());
        service.delete(500L);
        verify(reviewImageMapper).deleteByReviewId(500L);
        verify(reviewMapper).deleteById(500L);
        verify(productMapper).refreshReviewAggregates(1L);
    }

    @Test
    @DisplayName("后台删除不存在的评价 → 评价不存在")
    void deleteNotFound() {
        when(reviewMapper.getById(500L)).thenReturn(null);
        assertThatThrownBy(() -> service.delete(500L))
                .isInstanceOf(MallBusinessException.class)
                .hasMessage(MallMessageConstant.REVIEW_NOT_FOUND);
    }
}
