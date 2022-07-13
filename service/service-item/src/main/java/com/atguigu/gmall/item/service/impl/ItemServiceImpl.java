package com.atguigu.gmall.item.service.impl;

import com.alibaba.fastjson.JSON;
import com.atguigu.gmall.common.constant.RedisConst;
import com.atguigu.gmall.item.service.ItemService;
import com.atguigu.gmall.list.client.ListFeignClient;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.stream.Collectors;

/**
 * author:atGuiGu-mqx
 * date:2022/4/28 14:41
 * 描述：
 **/
@Service
public class ItemServiceImpl implements ItemService {

    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private RedissonClient redissonClient;

    @Autowired
    private ThreadPoolExecutor threadPoolExecutor;

    @Autowired
    private ListFeignClient listFeignClient;


    @Override
    public Map getItem(Long skuId) {
        HashMap<String, Object> hashMap = new HashMap<>();

        //  在这里利用布隆布隆过滤器判断！
        //  目的先放行其他的sku,
        //        RBloomFilter<Object> bloomFilter = redissonClient.getBloomFilter(RedisConst.SKU_BLOOM_FILTER);
        //        //  如果布隆过滤器中没有skuId 则直接返回。
        //        if (!bloomFilter.contains(skuId)){
        //            return hashMap;
        //        }

        //  创建一个线程 来执行 productFeignClient.getSkuInfo(skuId);
        //  supplyAsync 或 runAsync 区别在于看后续是否有应用到当前返回值！
        CompletableFuture<SkuInfo> skuInfoCompletableFuture = CompletableFuture.supplyAsync(() -> {
            //  获取skuInfo
            SkuInfo skuInfo = productFeignClient.getSkuInfo(skuId);
            //  存储到map 中.
            hashMap.put("skuInfo",skuInfo);
            return skuInfo;
        },threadPoolExecutor);

        //  thenAcceptAsync方法没有返回值   thenApplyAsync方法！ 有返回值
        CompletableFuture<Void> categoryView = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //  获取 分类数据
            BaseCategoryView baseCategoryView = productFeignClient.getCategoryView(skuInfo.getCategory3Id());
            //  存储数据
            hashMap.put("categoryView", baseCategoryView);
        },threadPoolExecutor);

        //  获取价格信息
        CompletableFuture<Void> priceCompletableFuture = CompletableFuture.runAsync(() -> {
            BigDecimal skuPrice = productFeignClient.getSkuPrice(skuId);
            hashMap.put("price", skuPrice);
        },threadPoolExecutor);

        CompletableFuture<Void> spuSaleAttrListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //  获取销售属性+锁定
            List<SpuSaleAttr> spuSaleAttrList = productFeignClient.getSpuSaleAttrListCheckBySku(skuId, skuInfo.getSpuId());
            hashMap.put("spuSaleAttrList", spuSaleAttrList);
        },threadPoolExecutor);

        CompletableFuture<Void> skuJsonCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //  获取切换功能数据
            Map skuValueIdsMap = productFeignClient.getSkuValueIdsMap(skuInfo.getSpuId());
            //  需要将这个map 转换为Json
            String skuJson = JSON.toJSONString(skuValueIdsMap);

            hashMap.put("valuesSkuJson",skuJson);
        },threadPoolExecutor);

        CompletableFuture<Void> spuPosterListCompletableFuture = skuInfoCompletableFuture.thenAcceptAsync(skuInfo -> {
            //  获取海报信息
            List<SpuPoster> spuPosterList = productFeignClient.getSpuPosterBySpuId(skuInfo.getSpuId());
            hashMap.put("spuPosterList", spuPosterList);
        },threadPoolExecutor);

        CompletableFuture<Void> paramCompletableFuture = CompletableFuture.runAsync(() -> {
            //  获取规则参数
            List<BaseAttrInfo> attrList = productFeignClient.getAttrList(skuId);

            List<HashMap<String, Object>> maps = attrList.stream().map(baseAttrInfo -> {
                HashMap<String, Object> map = new HashMap<>();
                map.put("attrName", baseAttrInfo.getAttrName());
                //  有点难度么? 0表示取第一条数据
                map.put("attrValue", baseAttrInfo.getAttrValueList().get(0).getValueName());
                return map;
            }).collect(Collectors.toList());
            //  规则参数：
            hashMap.put("skuAttrList", maps);
        },threadPoolExecutor);

        //  开一个线程.
        CompletableFuture<Void> voidCompletableFuture = CompletableFuture.runAsync(() -> {
            listFeignClient.incrHotScore(skuId);
        });

        //  多任务组合： allOf anyOf
        CompletableFuture.allOf(skuInfoCompletableFuture,
                categoryView,
                priceCompletableFuture,
                spuSaleAttrListCompletableFuture,
                skuJsonCompletableFuture,
                spuPosterListCompletableFuture,
                paramCompletableFuture,
                voidCompletableFuture
                ).join();

        return hashMap;
    }
}