package com.atguigu.gmall.activity.service;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.activity.SeckillGoods;
import com.atguigu.gmall.model.activity.UserRecode;

import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/5/20 14:01
 * 描述：
 **/
public interface SeckillGoodsService {

    //  查询所有秒杀列表:   hash :   hvals key;
    List<SeckillGoods> findAll();

    //  根据skuId 来获取到商品详情：hget key field;
    SeckillGoods getSeckillGoodsById(Long skuId);

    /**
     * 监听用户秒杀消息：实现预下单操作
     * @param userRecode
     */
    void seckillOrder(UserRecode userRecode);

    /**
     * 更新库存。
     * @param skuId
     */
    void updateStock(Long skuId);

    /**
     * 检查订单状态.
     * @param skuId
     * @param userId
     * @return
     */
    Result checkOrder(Long skuId, String userId);
}
