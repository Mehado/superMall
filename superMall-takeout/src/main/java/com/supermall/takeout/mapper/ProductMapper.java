package com.supermall.takeout.mapper;

import com.github.pagehelper.Page;
import com.supermall.takeout.dto.ProductPageQueryDTO;
import com.supermall.takeout.dto.ProductQueryDTO;
import com.supermall.takeout.entity.Product;
import com.supermall.takeout.vo.ProductVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ProductMapper {

    void insert(Product product);

    void update(Product product);

    void deleteById(@Param("id") Long id, @Param("merchantId") Long merchantId);

    Product getById(Long id);

    /** 商家端分页（merchantId 强制隔离） */
    Page<Product> pageQuery(ProductPageQueryDTO dto);

    /** 用户端：按商家/分类/关键字浏览上架商品 */
    Page<Product> pageQueryForUser(ProductQueryDTO dto);

    /** 首页商品流：从在售商品里随机抽取 limit 条（ORDER BY RAND()） */
    List<ProductVO> randomOnSale(@Param("keyword") String keyword,
                                 @Param("businessType") Integer businessType,
                                 @Param("limit") int limit);

    List<Product> listByCategoryId(@Param("categoryId") Long categoryId,
                                   @Param("merchantId") Long merchantId);

    int countByCategoryId(@Param("categoryId") Long categoryId, @Param("merchantId") Long merchantId);

    /** 下单时条件扣减 DB 库存（库存不足返回 0） */
    int deductStock(@Param("id") Long id, @Param("number") Integer number);

    /** 取消 / 关单时回补 DB 库存 */
    int releaseStock(@Param("id") Long id, @Param("number") Integer number);

    /** 订单完成后异步累加销量 */
    int addSales(@Param("id") Long id, @Param("number") Integer number);

    /** 启动预热：全部启用商品 */
    List<Product> listAll();
}
