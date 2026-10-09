package com.supermall.takeout.service.impl;

import com.supermall.constant.MessageConstant;
import com.supermall.constant.StatusConstant;
import com.supermall.context.BaseContext;
import com.supermall.takeout.dto.CartAddDTO;
import com.supermall.takeout.dto.CartUpdateDTO;
import com.supermall.takeout.entity.Cart;
import com.supermall.takeout.entity.CartItem;
import com.supermall.takeout.entity.Merchant;
import com.supermall.takeout.entity.Product;
import com.supermall.takeout.entity.Sku;
import com.supermall.takeout.exception.CartBusinessException;
import com.supermall.takeout.exception.MerchantBusinessException;
import com.supermall.takeout.exception.ProductBusinessException;
import com.supermall.takeout.mapper.CartItemMapper;
import com.supermall.takeout.mapper.CartMapper;
import com.supermall.takeout.mapper.MerchantMapper;
import com.supermall.takeout.mapper.ProductMapper;
import com.supermall.takeout.mapper.SkuMapper;
import com.supermall.takeout.service.CartService;
import com.supermall.takeout.vo.CartItemVO;
import com.supermall.takeout.vo.CartVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {

    private final CartMapper cartMapper;
    private final CartItemMapper cartItemMapper;
    private final ProductMapper productMapper;
    private final SkuMapper skuMapper;
    private final MerchantMapper merchantMapper;

    @Override
    @Transactional
    public void add(CartAddDTO dto) {
        Long userId = BaseContext.getCurrentId();
        Merchant merchant = merchantMapper.getById(dto.getMerchantId());
        if (merchant == null) {
            throw new MerchantBusinessException(MessageConstant.MERCHANT_NOT_FOUND);
        }
        // 商家休息中不允许加购（merchant 已从 DB 查出，含 business_status）
        if (StatusConstant.DISABLE.equals(merchant.getBusinessStatus())) {
            throw new MerchantBusinessException(MessageConstant.MERCHANT_CLOSED);
        }
        Product product = productMapper.getById(dto.getProductId());
        if (product == null || !dto.getMerchantId().equals(product.getMerchantId())) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
        }
        if (Integer.valueOf(0).equals(product.getStatus())) {
            throw new ProductBusinessException(MessageConstant.PRODUCT_OFF_SALE);
        }

        Long skuId = dto.getSkuId() == null ? 0L : dto.getSkuId();
        String skuName = null;
        BigDecimal price = product.getPrice();
        if (skuId > 0) {
            Sku sku = skuMapper.getById(skuId);
            if (sku == null || !sku.getProductId().equals(product.getId())) {
                throw new ProductBusinessException(MessageConstant.PRODUCT_NOT_FOUND);
            }
            skuName = sku.getName();
            price = sku.getPrice();
        }

        Cart cart = cartMapper.getByUserAndMerchant(userId, dto.getMerchantId());
        if (cart == null) {
            LocalDateTime now = LocalDateTime.now();
            cart = Cart.builder()
                    .userId(userId)
                    .merchantId(dto.getMerchantId())
                    .createTime(now)
                    .updateTime(now)
                    .build();
            cartMapper.insert(cart);
        }

        CartItem exist = cartItemMapper.getByProductAndSku(cart.getId(), product.getId(), skuId);
        if (exist != null) {
            cartItemMapper.updateNumber(exist.getId(), userId, exist.getNumber() + dto.getNumber());
            return;
        }

        CartItem item = CartItem.builder()
                .cartId(cart.getId())
                .userId(userId)
                .merchantId(dto.getMerchantId())
                .productId(product.getId())
                .skuId(skuId)
                .productName(product.getName())
                .skuName(skuName)
                .image(product.getImage())
                .price(price)
                .number(dto.getNumber())
                .createTime(LocalDateTime.now())
                .build();
        cartItemMapper.insert(item);
    }

    @Override
    public void updateNumber(CartUpdateDTO dto) {
        Long userId = BaseContext.getCurrentId();
        CartItem item = cartItemMapper.getById(dto.getId());
        if (item == null || !userId.equals(item.getUserId())) {
            throw new CartBusinessException(MessageConstant.CART_ITEM_NOT_FOUND);
        }
        if (dto.getNumber() == 0) {
            cartItemMapper.deleteById(dto.getId(), userId);
            return;
        }
        cartItemMapper.updateNumber(dto.getId(), userId, dto.getNumber());
    }

    @Override
    public void deleteItem(Long itemId) {
        Long userId = BaseContext.getCurrentId();
        CartItem item = cartItemMapper.getById(itemId);
        if (item == null || !userId.equals(item.getUserId())) {
            throw new CartBusinessException(MessageConstant.CART_ITEM_NOT_FOUND);
        }
        cartItemMapper.deleteById(itemId, userId);
    }

    @Override
    public void clearByMerchant(Long merchantId) {
        Long userId = BaseContext.getCurrentId();
        Cart cart = cartMapper.getByUserAndMerchant(userId, merchantId);
        if (cart != null) {
            cartItemMapper.deleteByCartId(cart.getId());
        }
    }

    @Override
    public void clearAll() {
        cartItemMapper.deleteByUserId(BaseContext.getCurrentId());
    }

    @Override
    public List<CartVO> list() {
        Long userId = BaseContext.getCurrentId();
        List<CartItem> items = cartItemMapper.listByUserId(userId);

        // 按商家分组，保持顺序
        Map<Long, List<CartItem>> grouped = new LinkedHashMap<>();
        for (CartItem item : items) {
            grouped.computeIfAbsent(item.getMerchantId(), k -> new ArrayList<>()).add(item);
        }

        List<CartVO> result = new ArrayList<>();
        for (Map.Entry<Long, List<CartItem>> entry : grouped.entrySet()) {
            Long merchantId = entry.getKey();
            Merchant merchant = merchantMapper.getById(merchantId);
            BigDecimal total = BigDecimal.ZERO;
            List<CartItemVO> voItems = new ArrayList<>();
            for (CartItem item : entry.getValue()) {
                BigDecimal amount = item.getPrice().multiply(BigDecimal.valueOf(item.getNumber()));
                total = total.add(amount);
                voItems.add(CartItemVO.builder()
                        .id(item.getId())
                        .productId(item.getProductId())
                        .skuId(item.getSkuId())
                        .productName(item.getProductName())
                        .skuName(item.getSkuName())
                        .image(item.getImage())
                        .price(item.getPrice())
                        .number(item.getNumber())
                        .amount(amount)
                        .build());
            }
            result.add(CartVO.builder()
                    .cartId(entry.getValue().get(0).getCartId())
                    .merchantId(merchantId)
                    .shopName(merchant == null ? null : merchant.getShopName())
                    .items(voItems)
                    .totalAmount(total)
                    .build());
        }
        return result;
    }

    @Override
    public List<CartItem> listByMerchant(Long merchantId) {
        return cartItemMapper.listByUserIdAndMerchant(BaseContext.getCurrentId(), merchantId);
    }
}
