package com.atguigu.gmall.product.api;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.*;
import com.atguigu.gmall.product.service.ManageService;
import org.apache.ibatis.annotations.Param;
import org.simpleframework.xml.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

/**
 * author:atGuiGu-mqx
 * date:2022/4/28 15:08
 * 描述：
 **/
@RestController
@RequestMapping("/api/product")
public class ProductApiController {

    //  注入服务层对象
    @Autowired
    private ManageService manageService;

    //  根据skuId 来获取skuInfo 数据
    //  /api/product/inner/getSkuInfo/{skuId}
    @GetMapping("inner/getSkuInfo/{skuId}")
    public SkuInfo getSkuInfo(@PathVariable Long skuId){
        //  调用服务层方法.
        return manageService.getSkuInfo(skuId);
    }

    //  根据三级分类Id 来获取分类数据.
    @GetMapping("inner/getCategoryView/{category3Id}")
    public BaseCategoryView getCategoryView(@PathVariable Long category3Id){
        //  调用服务层方法.
        return manageService.getCategoryView(category3Id);
    }

    //  获取价格数据.
    //  /api/product/inner/getSkuPrice/{skuId}
    @GetMapping("inner/getSkuPrice/{skuId}")
    public BigDecimal getSkuPrice(@PathVariable Long skuId){
        //  调用服务层方法.
        return manageService.getSkuPrice(skuId);
    }

    //  回显销售属性+锁定功能！
    @GetMapping("inner/getSpuSaleAttrListCheckBySku/{skuId}/{spuId}")
    public List<SpuSaleAttr> getSpuSaleAttrListCheckBySku(@PathVariable Long skuId ,
                                                          @PathVariable Long spuId){
        //  调用服务层方法.
        return manageService.getSpuSaleAttrListCheckBySku(skuId,spuId);
    }

    //  获取切换的数据
    @GetMapping("inner/getSkuValueIdsMap/{spuId}")
    public Map getSkuValueIdsMap(@PathVariable Long spuId){
        //  调用服务层方法.
        return manageService.getSkuValueIdsMap(spuId);
    }

    //  根据spuId 来获取海报信息
    @GetMapping("inner/findSpuPosterBySpuId/{spuId}")
    public List<SpuPoster> findSpuPosterBySpuId(@PathVariable Long spuId){
        //  调用服务层方法.
        return manageService.getSpuPosterBySpuId(spuId);
    }

    //  根据skuId 来获取平台属性数据
    @GetMapping("inner/getAttrList/{skuId}")
    public List<BaseAttrInfo> getAttrList(@PathVariable Long skuId){
        //  调用服务层方法.
        return manageService.getAttrList(skuId);
    }

    //  获取到首页分类数据
    @GetMapping("getBaseCategoryList")
    public Result getBaseCategoryList(){
        //  返回数据
        return Result.ok(manageService.getBaseCategoryList());
    }

    //  根据品牌Id 来获取品牌对象数据
    @GetMapping("inner/getTrademark/{tmId}")
    public BaseTrademark getTrademark(@PathVariable Long tmId){
        return manageService.getTrademarkByTmId(tmId);
    }
}