package com.atguigu.gmall.product.service;

import com.atguigu.gmall.model.product.BaseCategoryTrademark;
import com.atguigu.gmall.model.product.BaseTrademark;
import com.atguigu.gmall.model.product.CategoryTrademarkVo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface BaseCategoryTrademarkService extends IService<BaseCategoryTrademark> {
    /**
     * 根据分类Id 获取品牌数据.
     * @param category3Id
     * @return
     */
    List<BaseTrademark> getTrademarkList(Long category3Id);

    /**
     * 根据三级分类Id 查询可选的品牌列表
     * @param category3Id
     * @return
     */
    List<BaseTrademark> getCurrentTrademarkList(Long category3Id);

    /**
     * 保存数据
     * @param categoryTrademarkVo
     */
    void save(CategoryTrademarkVo categoryTrademarkVo);

    /**
     * 根据分类Id 与 品牌Id 进行删除数据
     * @param category3Id
     * @param trademarkId
     */
    void deleteById(Long category3Id, Long trademarkId);
}
