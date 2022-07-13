package com.atguigu.gmall.product.controller;

import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.model.product.BaseSaleAttr;
import com.atguigu.gmall.model.product.SpuImage;
import com.atguigu.gmall.model.product.SpuInfo;
import com.atguigu.gmall.model.product.SpuSaleAttr;
import com.atguigu.gmall.product.service.ManageService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.simpleframework.xml.Path;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * author:atGuiGu-mqx
 * date:2022/4/26 14:30
 * 描述：
 **/
@RestController
@RequestMapping("/admin/product")
public class SpuManageController {

    @Autowired
    private ManageService manageService;

    //  http://localhost/admin/product/1/10?category3Id=61
    //  springmvc 对象传值 SpuInfo 类  private Long category3Id;
    //  /admin/product/{page}/{limit}
    //  查询带分页的spuInfo
    //根据分类ID获取商品列表。
    @GetMapping("{page}/{limit}")
    public Result getSpuInfoList(@PathVariable Long page,
                                 @PathVariable Long limit,
                                 SpuInfo spuInfo
                                 ){
        //  创建Page 对象
        Page<SpuInfo> spuInfoPage = new Page<>(page,limit);
        //  调用服务层方法
        IPage<SpuInfo> iPage = manageService.getSpuInfoList(spuInfoPage,spuInfo);
        //  返回数据.
        return Result.ok(iPage);
    }

    //  /admin/product/baseSaleAttrList
    //查询所有的销售属性
    @GetMapping("baseSaleAttrList")
    public Result getSaleAttrList(){
        //  调用服务层方法
        List<BaseSaleAttr> baseSaleAttrList = manageService.getSaleAttrList();
        return Result.ok(baseSaleAttrList);
    }

    //  保存spuInfo
    //  /admin/product/saveSpuInfo
    //  Json ---> JavaObject
    @PostMapping("saveSpuInfo")
    public Result saveSpuInfo(@RequestBody SpuInfo spuInfo){
        //  调用服务层方法
        this.manageService.saveSpuInfo(spuInfo);
        //  默认返回.
        return Result.ok();
    }

    //  根据spuId 来获取spuImage 集合
    //  /admin/product/spuImageList/{spuId}
    @GetMapping("spuImageList/{spuId}")
    public Result getSpuImageList(@PathVariable Long spuId){
        //  调用服务层方法.
        List<SpuImage> spuImageList = this.manageService.getSpuImageList(spuId);
        return Result.ok(spuImageList);
    }

    // 根据 spuId 来获取到销售属性+销售属性值集合！
    //  /admin/product/spuSaleAttrList/{spuId}
    @GetMapping("spuSaleAttrList/{spuId}")
    public Result getSpuSaleAttrList(@PathVariable Long spuId){
        //  调用服务层方法. 返回的结果集是谁！
        List<SpuSaleAttr> spuSaleAttrList =  this.manageService.getSpuSaleAttrList(spuId);
        //  返回数据
        return Result.ok(spuSaleAttrList);
    }
}