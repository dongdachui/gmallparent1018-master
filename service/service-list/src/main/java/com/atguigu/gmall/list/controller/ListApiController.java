package com.atguigu.gmall.list.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.list.service.SearchService;
import com.atguigu.gmall.model.list.Goods;
import com.atguigu.gmall.model.list.SearchParam;
import com.atguigu.gmall.model.list.SearchResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.ElasticsearchRestTemplate;
import org.springframework.data.elasticsearch.core.ElasticsearchTemplate;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

/**
 * author:atGuiGu-mqx
 * date:2022/5/6 11:41
 * 描述：
 **/
@RequestMapping("api/list")
@RestController
public class ListApiController {

    //  设计模式：jdbcTemplate  工厂，单例，适配，模板，代理
    // private ElasticsearchTemplate elasticsearchTemplate;  需要自己编写配置.
    @Autowired
    private ElasticsearchRestTemplate elasticsearchRestTemplate;

    @Autowired
    private SearchService searchService;

    //  自定义一个控制器
    @GetMapping("createIndex")
    public Result createIndex(){
        //  类 Goods
        elasticsearchRestTemplate.createIndex(Goods.class);
        //  创建mapping！
        elasticsearchRestTemplate.putMapping(Goods.class);
        //  默认返回
        return Result.ok();
    }

    //  商品上架
    @GetMapping("inner/upperGoods/{skuId}")
    public Result upperGoods(@PathVariable Long skuId){
        //  调用服务层方法
        searchService.onSale(skuId);
        return Result.ok();
    }

    //  商品下架
    @GetMapping("inner/lowerGoods/{skuId}")
    public Result lowerGoods(@PathVariable Long skuId){
        //  调用服务层方法
        searchService.cancelSale(skuId);
        return Result.ok();
    }

    //  发布到feign 上.
    @GetMapping("inner/incrHotScore/{skuId}")
    public Result incrHotScore(@PathVariable Long skuId){
        //  调用服务层方法。
        searchService.incrHotScore(skuId);
        return Result.ok();
    }

    //  编写检索控制器！
    @PostMapping
    public Result list(@RequestBody SearchParam searchParam){
        //  调用检索方法
        SearchResponseVo search = null;
        try {
            search = searchService.search(searchParam);
        } catch (IOException e) {
            e.printStackTrace();
        }
        //  返回数据
        return Result.ok(search);
    }


}