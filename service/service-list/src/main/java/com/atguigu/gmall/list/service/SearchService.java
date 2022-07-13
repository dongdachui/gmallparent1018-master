package com.atguigu.gmall.list.service;

import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;

import java.io.IOException;
import java.util.List;

public interface SearchService {

    // Optional<T> findById(ID id);
    //  商品上架时，要根据skuId;
    void onSale(Long skuId);
    //  批量上架
    void onSale(List<Long> skuIds);

    //  商品下架时，根据skuId;
    void cancelSale(Long skuId);
    //  批量下架
    void cancelSale(List<Long> skuIds);

    /**
     * 更新商品的热度排名
     * @param skuId
     */
    void incrHotScore(Long skuId);


    /**
     *  查询
     * @param searchParam
     * @return
     */
    SearchResponseVo search(SearchParam searchParam) throws IOException;
}
