package com.atguigu.gmall.cart.service;

import com.atguigu.gmall.model.cart.CartInfo;

import java.util.List;

public interface CartService {

    /**
     *
     * @param skuId
     * @param userId
     * @param skuNum
     */
    void addToCart(Long skuId, String userId, Integer skuNum);

    /**
     * 查看购物车列表.
     * @param userId
     * @param userTempId
     * @return
     */
    List<CartInfo> getCartList(String userId, String userTempId);

    /**
     * 更改选中状态.
     * @param skuId
     * @param userId
     * @param isChecked
     */
    void checkCart(Long skuId, String userId, Integer isChecked);

    /**
     * 删除购物项
     * @param skuId
     * @param userId
     */
    void deleteCart(Long skuId, String userId);

    /**
     * 根据userId 查询选中状态的购物车列表
     * @param userId
     * @return
     */
    List<CartInfo> getCartCheckedList(String userId);

}
