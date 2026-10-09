package com.supermall.takeout.vo;

import com.supermall.takeout.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@EqualsAndHashCode(callSuper = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ProductVO extends Product implements Serializable {

    /** 分类名称 */
    private String categoryName;

    /** 店铺名称（用户端浏览时展示） */
    private String shopName;

    /** 规格列表，为空表示无规格商品 */
    private List<SkuVO> skuList;
}
